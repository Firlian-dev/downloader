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

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {
    private final ProviderDetector providerDetector;
    private final MediaDownloader mediaDownloader;
    private final CacheRepository cacheRepository;
    private final TaskQueue taskQueue;

    public Mono<MediaContent> processUrl(String url, Long chatId) {
        log.info("Processing URL: {} for chat: {}", url, chatId);

        // Check cache first
        var cachedContent = cacheRepository.get(url);
        if (cachedContent.isPresent()) {
            log.info("Found cached content for URL: {}", url);
            return Mono.just(cachedContent.get());
        }

        // Detect provider
        Provider provider = providerDetector.detectProvider(url);
        if (provider == Provider.UNKNOWN) {
            log.error("Unsupported provider for URL: {}", url);
            throw new UnsupportedProviderException("Источник не поддерживается");
        }

        // Check if task already exists
        var existingTask = taskQueue.getTask(url);
        if (existingTask.isPresent() && existingTask.get().getStatus() == TaskStatus.DOWNLOADING) {
            log.info("Task already in progress for URL: {}", url);
            return Mono.error(new DownloadException("Загрузка уже выполняется"));
        }

        // Create and add task
        DownloadTask task = DownloadTask.builder()
                .url(url)
                .provider(provider)
                .chatId(chatId)
                .status(TaskStatus.PENDING)
                .build();

        if (!taskQueue.addTask(task)) {
            log.warn("Failed to add task for URL: {}", url);
        }

        // Download content
        return mediaDownloader.download(url)
                .doOnNext(content -> {
                    log.info("Downloaded content from URL: {}, size: {} bytes", url, content.getSizeBytes());
                    cacheRepository.put(url, content);
                    taskQueue.completeTask(url);
                })
                .doOnError(error -> {
                    log.error("Failed to download from URL: {}", url, error);
                    taskQueue.failTask(url, error.getMessage());
                })
                .onErrorMap(this::mapError);
    }

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
