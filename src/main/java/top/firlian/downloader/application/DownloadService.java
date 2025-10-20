package top.firlian.downloader.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import top.firlian.downloader.domain.error.ContentUnavailableException;
import top.firlian.downloader.domain.error.DownloadException;
import top.firlian.downloader.domain.error.UnsupportedProviderException;
import top.firlian.downloader.domain.model.*;
import top.firlian.downloader.domain.port.CacheRepository;
import top.firlian.downloader.domain.port.MediaDownloader;
import top.firlian.downloader.domain.port.ProviderDetector;
import top.firlian.downloader.domain.port.TaskQueue;

/**
 * Сервис обработки загрузки медиа контента.
 * Координирует процесс загрузки: проверяет кэш, определяет провайдера,
 * управляет очередью задач и выполняет загрузку файлов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {
    private final ProviderDetector providerDetector;
    private final MediaDownloader mediaDownloader;
    private final CacheRepository cacheRepository;
    private final TaskQueue taskQueue;

    /**
     * Обрабатывает URL для загрузки медиа контента.
     * Выполняет следующие шаги:
     * 1. Проверяет наличие контента в кэше
     * 2. Определяет провайдера контента
     * 3. Проверяет наличие активной задачи загрузки
     * 4. Создает новую задачу и выполняет загрузку
     *
     * @param url URL для загрузки
     * @param chatId ID чата Telegram, запросившего загрузку
     * @return Mono с загруженным медиа контентом
     */
    public Mono<MediaContent> processUrl(String url, Long chatId) {
        log.info("Processing URL: {} for chat: {}", url, chatId);

        // Сначала проверяем кэш - если контент уже загружен, возвращаем его
        var cachedContent = cacheRepository.get(url);
        if (cachedContent.isPresent()) {
            log.info("Found cached content for URL: {}", url);
            return Mono.just(cachedContent.get());
        }

        // Определяем провайдера (YouTube, VK, Instagram и т.д.)
        Provider provider = providerDetector.detectProvider(url);
        if (provider == Provider.UNKNOWN) {
            log.error("Unsupported provider for URL: {}", url);
            throw new UnsupportedProviderException("Источник не поддерживается");
        }

        // Проверяем, не выполняется ли уже загрузка этого URL
        var existingTask = taskQueue.getTask(url);
        if (existingTask.isPresent() && existingTask.get().getStatus() == TaskStatus.DOWNLOADING) {
            log.info("Task already in progress for URL: {}", url);
            return Mono.error(new DownloadException("Загрузка уже выполняется"));
        }

        // Создаем новую задачу в очереди
        DownloadTask task = DownloadTask.builder()
                .url(url)
                .provider(provider)
                .chatId(chatId)
                .status(TaskStatus.PENDING)
                .build();

        if (!taskQueue.addTask(task)) {
            log.warn("Failed to add task for URL: {}", url);
        }

        // Выполняем загрузку и обрабатываем результат
        return mediaDownloader.download(url)
                .doOnNext(content -> {
                    log.info("Downloaded content from URL: {}, size: {} bytes", url, content.getSizeBytes());
                    // Сохраняем в кэш для повторного использования
                    cacheRepository.put(url, content);
                    // Отмечаем задачу как завершенную
                    taskQueue.completeTask(url);
                })
                .doOnError(error -> {
                    log.error("Failed to download from URL: {}", url, error);
                    // Отмечаем задачу как проваленную с сообщением об ошибке
                    taskQueue.failTask(url, error.getMessage());
                })
                .onErrorMap(this::mapError);
    }

    /**
     * Обрабатывает URL для загрузки конкретного элемента из плейлиста или карусели.
     *
     * @param url URL плейлиста или карусели
     * @param itemIndex индекс элемента для загрузки (начиная с 0)
     * @param chatId ID чата Telegram
     * @return Mono с загруженным элементом
     */
    public Mono<MediaContent> processUrlWithIndex(String url, int itemIndex, Long chatId) {
        log.info("Processing URL: {} with item index: {} for chat: {}", url, itemIndex, chatId);

        Provider provider = providerDetector.detectProvider(url);
        if (provider == Provider.UNKNOWN) {
            throw new UnsupportedProviderException("Источник не поддерживается");
        }

        return mediaDownloader.downloadSpecificItem(url, itemIndex)
                .doOnNext(content -> {
                    log.info("Downloaded item {} from URL: {}", itemIndex, url);
                })
                .onErrorMap(this::mapError);
    }

    /**
     * Преобразует исключения в соответствующие доменные исключения.
     * Сохраняет специфичные исключения и оборачивает остальные в DownloadException.
     *
     * @param error исходное исключение
     * @return преобразованное исключение
     */
    private Throwable mapError(Throwable error) {
        if (error instanceof ContentUnavailableException) {
            return error;
        }
        if (error instanceof UnsupportedProviderException) {
            return error;
        }
        return new DownloadException("Ошибка загрузки. Попробуйте позже", error);
    }
}
