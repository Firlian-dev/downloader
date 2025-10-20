package top.firlian.downloader.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import top.firlian.downloader.application.DownloadService;
import top.firlian.downloader.domain.error.ContentUnavailableException;
import top.firlian.downloader.domain.error.DownloadException;
import top.firlian.downloader.domain.error.UnsupportedProviderException;
import top.firlian.downloader.domain.model.MediaContent;
import top.firlian.downloader.domain.model.MediaType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBotAdapter extends TelegramLongPollingBot {

    private final DownloadService downloadService;
    private final String botUsername;
    private final long sizeLimitBytes;

    public TelegramBotAdapter(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${downloader.size-limit-mb:50}") int sizeLimitMb,
            DownloadService downloadService) {
        super(botToken);
        this.botUsername = botUsername;
        this.downloadService = downloadService;
        this.sizeLimitBytes = sizeLimitMb * 1024L * 1024L;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/start")) {
                handleStartCommand(chatId);
            } else if (isUrl(messageText)) {
                handleUrlMessage(chatId, messageText);
            } else {
                sendTextMessage(chatId, "Пожалуйста, отправьте ссылку на медиа из YouTube, VK или Instagram.");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            handleCallbackQuery(chatId, callbackData);
        }
    }

    private void handleStartCommand(Long chatId) {
        String welcomeMessage = "Привет! Я бот для скачивания медиа.\n\n" +
                "Отправьте мне ссылку на:\n" +
                "- YouTube видео или шорты\n" +
                "- VK посты, фото, документы или видео\n" +
                "- Instagram посты, Reels или карусели\n\n" +
                "Я скачаю медиа и отправлю вам файл или ссылку для скачивания.";
        sendTextMessage(chatId, welcomeMessage);
    }

    private void handleUrlMessage(Long chatId, String url) {
        log.info("Received URL: {} from chat: {}", url, chatId);
        
        sendTextMessage(chatId, "Начинаю загрузку...");

        downloadService.processUrl(url, chatId)
                .subscribe(
                        content -> handleDownloadedContent(chatId, content),
                        error -> handleDownloadError(chatId, error)
                );
    }

    private void handleCallbackQuery(Long chatId, String callbackData) {
        if (callbackData.startsWith("item:")) {
            String[] parts = callbackData.split(":");
            if (parts.length == 3) {
                String url = parts[1];
                int itemIndex = Integer.parseInt(parts[2]);
                
                log.info("User selected item {} for URL: {}", itemIndex, url);
                sendTextMessage(chatId, "Загружаю выбранный элемент...");
                
                downloadService.processUrlWithIndex(url, itemIndex, chatId)
                        .subscribe(
                                content -> handleDownloadedContent(chatId, content),
                                error -> handleDownloadError(chatId, error)
                        );
            }
        }
    }

    private void handleDownloadedContent(Long chatId, MediaContent content) {
        try {
            if (content.getItems() != null && !content.getItems().isEmpty()) {
                // Multiple items - show selection keyboard
                sendMediaSelectionKeyboard(chatId, content);
            } else if (content.getSizeBytes() > sizeLimitBytes) {
                // File too large - send URL
                sendTextMessage(chatId, 
                        "Файл слишком большой для прямой отправки (" + 
                        formatFileSize(content.getSizeBytes()) + ").\n\n" +
                        "Путь к файлу: " + content.getFilePath());
                log.info("File too large, sent path instead: {}", content.getFilePath());
            } else {
                // Send file directly
                sendMediaFile(chatId, content);
                log.info("Sent file to chat: {}, path: {}", chatId, content.getFilePath());
            }
        } catch (Exception e) {
            log.error("Error handling downloaded content", e);
            sendTextMessage(chatId, "Ошибка при отправке файла.");
        }
    }

    private void handleDownloadError(Long chatId, Throwable error) {
        String errorMessage;
        if (error instanceof ContentUnavailableException) {
            errorMessage = "Контент недоступен";
        } else if (error instanceof UnsupportedProviderException) {
            errorMessage = "Источник не поддерживается";
        } else if (error instanceof DownloadException) {
            errorMessage = error.getMessage();
        } else {
            errorMessage = "Ошибка загрузки. Попробуйте позже";
        }
        
        log.error("Download error for chat {}: {}", chatId, errorMessage, error);
        sendTextMessage(chatId, errorMessage);
    }

    private void sendMediaSelectionKeyboard(Long chatId, MediaContent content) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Найдено несколько элементов. Выберите, что скачать:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = 0; i < Math.min(content.getItems().size(), 10); i++) {
            var item = content.getItems().get(i);
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText((i + 1) + ". " + item.getTitle());
            button.setCallbackData("item:" + content.getUrl() + ":" + i);
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending selection keyboard", e);
        }
    }

    private void sendMediaFile(Long chatId, MediaContent content) {
        File file = new File(content.getFilePath());
        InputFile inputFile = new InputFile(file);

        try {
            switch (content.getType()) {
                case VIDEO -> {
                    SendVideo sendVideo = new SendVideo();
                    sendVideo.setChatId(chatId.toString());
                    sendVideo.setVideo(inputFile);
                    sendVideo.setCaption(content.getTitle());
                    execute(sendVideo);
                }
                case PHOTO -> {
                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setChatId(chatId.toString());
                    sendPhoto.setPhoto(inputFile);
                    sendPhoto.setCaption(content.getTitle());
                    execute(sendPhoto);
                }
                default -> {
                    SendDocument sendDocument = new SendDocument();
                    sendDocument.setChatId(chatId.toString());
                    sendDocument.setDocument(inputFile);
                    sendDocument.setCaption(content.getTitle());
                    execute(sendDocument);
                }
            }
        } catch (TelegramApiException e) {
            log.error("Error sending media file", e);
            sendTextMessage(chatId, "Ошибка при отправке файла. Путь: " + content.getFilePath());
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending text message", e);
        }
    }

    private boolean isUrl(String text) {
        return text != null && (text.startsWith("http://") || text.startsWith("https://"));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
