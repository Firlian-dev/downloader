package top.firlian.downloader.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.port.CacheRepository;

/**
 * Планировщик для автоматической очистки устаревших записей кэша.
 * Выполняется периодически по расписанию для освобождения памяти.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {

    private final CacheRepository cacheRepository;

    /**
     * Выполняет периодическую очистку истекших записей кэша.
     * Интервал выполнения настраивается через свойство
     * {@code downloader.cache-eviction-interval-ms} (по умолчанию 1 час).
     */
    @Scheduled(fixedRateString = "${downloader.cache-eviction-interval-ms:3600000}")
    public void evictExpiredCache() {
        log.info("Running scheduled cache eviction");
        cacheRepository.evictExpired();
    }
}
