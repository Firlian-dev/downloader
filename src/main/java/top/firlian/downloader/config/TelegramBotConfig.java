package top.firlian.downloader.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import top.firlian.downloader.adapter.TelegramBotAdapter;

/**
 * Конфигурация для регистрации Telegram бота.
 * Обеспечивает правильную инициализацию и регистрацию бота при запуске приложения.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramBotConfig {

    private final TelegramBotAdapter telegramBotAdapter;

    /**
     * Регистрирует Telegram бота после инициализации Spring контекста.
     * Метод вызывается автоматически при завершении инициализации приложения.
     *
     * @param event событие обновления контекста
     */
    @EventListener({ContextRefreshedEvent.class})
    public void init(ContextRefreshedEvent event) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBotAdapter);
            log.info("Telegram бот успешно зарегистрирован: {}", telegramBotAdapter.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("Ошибка регистрации Telegram бота", e);
            throw new RuntimeException("Не удалось зарегистрировать Telegram бота", e);
        }
    }
}
