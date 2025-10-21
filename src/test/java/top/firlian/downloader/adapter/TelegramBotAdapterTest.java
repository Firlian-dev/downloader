package top.firlian.downloader.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;
import top.firlian.downloader.application.DownloadService;
import top.firlian.downloader.domain.model.MediaContent;
import top.firlian.downloader.domain.model.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тесты для TelegramBotAdapter для проверки обработки команд и URL.
 */
class TelegramBotAdapterTest {

    @Mock
    private DownloadService downloadService;

    private TelegramBotAdapter telegramBotAdapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        telegramBotAdapter = new TelegramBotAdapter(
                "test-token",
                "test_bot",
                50,
                downloadService
        );
    }

    @Test
    void shouldHandleStartCommand() {
        // Подготовка
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        when(message.getChatId()).thenReturn(12345L);
        when(message.getChat()).thenReturn(chat);
        when(chat.getId()).thenReturn(12345L);

        // Выполнение
        telegramBotAdapter.onUpdateReceived(update);

        // Проверка - команда /start не должна вызывать загрузку
        verify(downloadService, never()).processUrl(anyString(), anyLong());
    }

    @Test
    void shouldHandleYouTubeUrl() {
        // Подготовка
        String youtubeUrl = "https://www.youtube.com/watch?v=test123";
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(youtubeUrl);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getChat()).thenReturn(chat);
        when(chat.getId()).thenReturn(12345L);

        MediaContent mockContent = MediaContent.builder()
                .url(youtubeUrl)
                .title("Test Video")
                .type(MediaType.VIDEO)
                .filePath("/tmp/test.mp4")
                .sizeBytes(1000L)
                .build();

        when(downloadService.processUrl(anyString(), anyLong()))
                .thenReturn(Mono.just(mockContent));

        // Выполнение
        telegramBotAdapter.onUpdateReceived(update);

        // Проверка - URL должен быть обработан
        verify(downloadService, times(1)).processUrl(eq(youtubeUrl), eq(12345L));
    }

    @Test
    void shouldHandleInstagramUrl() {
        // Подготовка
        String instagramUrl = "https://www.instagram.com/p/ABC123/";
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(instagramUrl);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getChat()).thenReturn(chat);
        when(chat.getId()).thenReturn(12345L);

        MediaContent mockContent = MediaContent.builder()
                .url(instagramUrl)
                .title("Test Instagram Post")
                .type(MediaType.PHOTO)
                .filePath("/tmp/test.jpg")
                .sizeBytes(1000L)
                .build();

        when(downloadService.processUrl(anyString(), anyLong()))
                .thenReturn(Mono.just(mockContent));

        // Выполнение
        telegramBotAdapter.onUpdateReceived(update);

        // Проверка - Instagram URL должен быть обработан
        verify(downloadService, times(1)).processUrl(eq(instagramUrl), eq(12345L));
    }

    @Test
    void shouldHandleVkUrl() {
        // Подготовка
        String vkUrl = "https://vk.com/wall-12345_67890";
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(vkUrl);
        when(message.getChatId()).thenReturn(12345L);
        when(message.getChat()).thenReturn(chat);
        when(chat.getId()).thenReturn(12345L);

        MediaContent mockContent = MediaContent.builder()
                .url(vkUrl)
                .title("Test VK Post")
                .type(MediaType.VIDEO)
                .filePath("/tmp/test.mp4")
                .sizeBytes(1000L)
                .build();

        when(downloadService.processUrl(anyString(), anyLong()))
                .thenReturn(Mono.just(mockContent));

        // Выполнение
        telegramBotAdapter.onUpdateReceived(update);

        // Проверка - VK URL должен быть обработан
        verify(downloadService, times(1)).processUrl(eq(vkUrl), eq(12345L));
    }

    @Test
    void shouldRejectNonUrlText() {
        // Подготовка
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("просто текст без URL");
        when(message.getChatId()).thenReturn(12345L);
        when(message.getChat()).thenReturn(chat);
        when(chat.getId()).thenReturn(12345L);

        // Выполнение
        telegramBotAdapter.onUpdateReceived(update);

        // Проверка - не-URL не должен вызывать загрузку
        verify(downloadService, never()).processUrl(anyString(), anyLong());
    }
}
