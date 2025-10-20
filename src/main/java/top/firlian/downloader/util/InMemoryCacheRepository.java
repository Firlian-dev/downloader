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

@Slf4j
@Component
public class InMemoryCacheRepository implements CacheRepository {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long cacheTtlMillis;

    public InMemoryCacheRepository(@Value("${downloader.cache-ttl-hours:24}") int cacheTtlHours) {
        this.cacheTtlMillis = cacheTtlHours * 60 * 60 * 1000L;
    }

    @Override
    public Optional<MediaContent> get(String url) {
        CacheEntry entry = cache.get(url);
        if (entry == null) {
            return Optional.empty();
        }

        if (Instant.now().toEpochMilli() - entry.timestamp > cacheTtlMillis) {
            cache.remove(url);
            return Optional.empty();
        }

        return Optional.of(entry.content);
    }

    @Override
    public void put(String url, MediaContent content) {
        cache.put(url, new CacheEntry(content, Instant.now().toEpochMilli()));
        log.debug("Cached content for URL: {}", url);
    }

    @Override
    public void evictExpired() {
        long now = Instant.now().toEpochMilli();
        cache.entrySet().removeIf(entry -> 
            now - entry.getValue().timestamp > cacheTtlMillis
        );
        log.info("Evicted expired cache entries");
    }

    private record CacheEntry(MediaContent content, long timestamp) {}
}
