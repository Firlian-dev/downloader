package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.MediaContent;

import java.util.Optional;

public interface CacheRepository {
    Optional<MediaContent> get(String url);
    void put(String url, MediaContent content);
    void evictExpired();
}
