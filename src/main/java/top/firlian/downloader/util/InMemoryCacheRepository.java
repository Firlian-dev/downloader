package top.firlian.downloader.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.model.MediaContent;
import top.firlian.downloader.domain.port.CacheRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация репозитория кэша медиа контента.
 * Хранит загруженные файлы в памяти с учетом времени жизни (TTL).
 * Потокобезопасная реализация с использованием ConcurrentHashMap.
 */
@Slf4j
@Component
public class InMemoryCacheRepository implements CacheRepository {

    /** Хранилище кэша с URL в качестве ключа */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    /** Время жизни записей в кэше в миллисекундах */
    private final long cacheTtlMillis;

    /**
     * Конструктор с настройкой времени жизни кэша.
     *
     * @param cacheTtlHours время жизни кэша в часах (по умолчанию 24)
     */
    public InMemoryCacheRepository(@Value("${downloader.cache-ttl-hours:24}") int cacheTtlHours) {
        this.cacheTtlMillis = cacheTtlHours * 60 * 60 * 1000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MediaContent> get(String url) {
        CacheEntry entry = cache.get(url);
        if (entry == null) {
            return Optional.empty();
        }

        // Проверяем, не истек ли срок жизни записи
        if (Instant.now().toEpochMilli() - entry.timestamp > cacheTtlMillis) {
            cache.remove(url);
            return Optional.empty();
        }

        return Optional.of(entry.content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String url, MediaContent content) {
        cache.put(url, new CacheEntry(content, Instant.now().toEpochMilli()));
        log.debug("Контент кэширован для URL: {}", url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evictExpired() {
        long now = Instant.now().toEpochMilli();
        // Удаляем все записи, у которых истек срок жизни
        cache.entrySet().removeIf(entry -> 
            now - entry.getValue().timestamp > cacheTtlMillis
        );
        log.info("Удалены устаревшие записи кэша");
    }

    /**
     * Запись в кэше с временной меткой.
     *
     * @param content медиа контент
     * @param timestamp время добавления в кэш в миллисекундах
     */
    private record CacheEntry(MediaContent content, long timestamp) {}
}
