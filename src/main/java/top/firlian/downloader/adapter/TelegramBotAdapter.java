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

/**
 * Адаптер Telegram бота для загрузки медиа контента.
 * Обрабатывает входящие сообщения от пользователей, координирует загрузку
 * медиа файлов и отправляет результаты обратно в Telegram.
 */
@Slf4j
@Component
public class TelegramBotAdapter extends TelegramLongPollingBot {

    private final DownloadService downloadService;
    private final String botUsername;
    
    /** Максимальный размер файла для прямой отправки в Telegram (в байтах) */
    private final long sizeLimitBytes;

    /**
     * Конструктор адаптера Telegram бота.
     *
     * @param botToken токен бота, полученный от BotFather
     * @param botUsername имя пользователя бота
     * @param sizeLimitMb лимит размера файла в МБ для прямой отправки (по умолчанию 50)
     * @param downloadService сервис для обработки загрузки медиа
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Обрабатывает входящие обновления от Telegram.
     * Поддерживает текстовые сообщения и callback запросы от inline кнопок.
     *
     * @param update обновление от Telegram API
     */
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

    /**
     * Обрабатывает команду /start.
     * Отправляет приветственное сообщение с инструкциями по использованию.
     *
     * @param chatId ID чата для отправки сообщения
     */
    private void handleStartCommand(Long chatId) {
        String welcomeMessage = "Привет! Я бот для скачивания медиа.\n\n" +
                "Отправьте мне ссылку на:\n" +
                "- YouTube видео или шорты\n" +
                "- VK посты, фото, документы или видео\n" +
                "- Instagram посты, Reels или карусели\n\n" +
                "Я скачаю медиа и отправлю вам файл или ссылку для скачивания.";
        sendTextMessage(chatId, welcomeMessage);
    }

    /**
     * Обрабатывает URL сообщение от пользователя.
     * Инициирует процесс загрузки медиа контента.
     *
     * @param chatId ID чата
     * @param url URL для загрузки
     */
    private void handleUrlMessage(Long chatId, String url) {
        log.info("Получен URL: {} от чата: {}", url, chatId);
        
        sendTextMessage(chatId, "Начинаю загрузку...");

        // Запускаем асинхронную загрузку с обработкой результата
        downloadService.processUrl(url, chatId)
                .subscribe(
                        content -> handleDownloadedContent(chatId, content),
                        error -> handleDownloadError(chatId, error)
                );
    }

    /**
     * Обрабатывает callback запросы от inline кнопок.
     * Используется для выбора конкретного элемента из плейлиста/карусели.
     *
     * @param chatId ID чата
     * @param callbackData данные callback в формате "item:URL:INDEX"
     */
    private void handleCallbackQuery(Long chatId, String callbackData) {
        if (callbackData.startsWith("item:")) {
            String[] parts = callbackData.split(":");
            if (parts.length == 3) {
                String url = parts[1];
                int itemIndex = Integer.parseInt(parts[2]);
                
                log.info("Пользователь выбрал элемент {} для URL: {}", itemIndex, url);
                sendTextMessage(chatId, "Загружаю выбранный элемент...");
                
                // Загружаем выбранный элемент
                downloadService.processUrlWithIndex(url, itemIndex, chatId)
                        .subscribe(
                                content -> handleDownloadedContent(chatId, content),
                                error -> handleDownloadError(chatId, error)
                        );
            }
        }
    }

    /**
     * Обрабатывает успешно загруженный контент.
     * В зависимости от типа контента и его размера:
     * - Отправляет inline клавиатуру для выбора элемента (если несколько элементов)
     * - Отправляет путь к файлу (если файл слишком большой)
     * - Отправляет файл напрямую (в остальных случаях)
     *
     * @param chatId ID чата для отправки результата
     * @param content загруженный медиа контент
     */
    private void handleDownloadedContent(Long chatId, MediaContent content) {
        try {
            if (content.getItems() != null && !content.getItems().isEmpty()) {
                // Несколько элементов - показываем клавиатуру выбора
                sendMediaSelectionKeyboard(chatId, content);
            } else if (content.getSizeBytes() > sizeLimitBytes) {
                // Файл слишком большой - отправляем только путь
                sendTextMessage(chatId, 
                        "Файл слишком большой для прямой отправки (" + 
                        formatFileSize(content.getSizeBytes()) + ").\n\n" +
                        "Путь к файлу: " + content.getFilePath());
                log.info("Файл слишком большой, отправлен путь: {}", content.getFilePath());
            } else {
                // Отправляем файл напрямую
                sendMediaFile(chatId, content);
                log.info("Файл отправлен в чат: {}, путь: {}", chatId, content.getFilePath());
            }
        } catch (Exception e) {
            log.error("Ошибка обработки загруженного контента", e);
            sendTextMessage(chatId, "Ошибка при отправке файла.");
        }
    }

    /**
     * Обрабатывает ошибки загрузки.
     * Преобразует технические исключения в понятные пользователю сообщения.
     *
     * @param chatId ID чата для отправки сообщения об ошибке
     * @param error возникшая ошибка
     */
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
        
        log.error("Ошибка загрузки для чата {}: {}", chatId, errorMessage, error);
        sendTextMessage(chatId, errorMessage);
    }

    /**
     * Отправляет inline клавиатуру для выбора элемента из плейлиста/карусели.
     * Отображает до 10 первых элементов с их названиями.
     *
     * @param chatId ID чата
     * @param content медиа контент с несколькими элементами
     */
    private void sendMediaSelectionKeyboard(Long chatId, MediaContent content) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Найдено несколько элементов. Выберите, что скачать:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Создаем кнопки для каждого элемента (максимум 10)
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
            log.error("Ошибка отправки клавиатуры выбора", e);
        }
    }

    /**
     * Отправляет медиа файл в чат.
     * Выбирает подходящий тип отправки в зависимости от типа медиа:
     * - SendVideo для видео
     * - SendPhoto для фото
     * - SendDocument для остальных типов
     *
     * @param chatId ID чата
     * @param content медиа контент для отправки
     */
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
            log.error("Ошибка отправки медиа файла", e);
            sendTextMessage(chatId, "Ошибка при отправке файла. Путь: " + content.getFilePath());
        }
    }

    /**
     * Отправляет текстовое сообщение в чат.
     *
     * @param chatId ID чата
     * @param text текст сообщения
     */
    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки текстового сообщения", e);
        }
    }

    /**
     * Проверяет, является ли текст URL.
     *
     * @param text текст для проверки
     * @return true если текст начинается с http:// или https://
     */
    private boolean isUrl(String text) {
        return text != null && (text.startsWith("http://") || text.startsWith("https://"));
    }

    /**
     * Форматирует размер файла в человекочитаемый формат.
     * Преобразует байты в B, KB, MB или GB в зависимости от размера.
     *
     * @param bytes размер в байтах
     * @return отформатированная строка с размером файла
     */
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
