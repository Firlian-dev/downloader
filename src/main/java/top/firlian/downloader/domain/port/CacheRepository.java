package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.MediaContent;

import java.util.Optional;

/**
 * Интерфейс репозитория для кэширования медиа контента.
 * Обеспечивает временное хранение загруженных файлов для повторного использования.
 */
public interface CacheRepository {
    /**
     * Получает закэшированный контент по URL.
     *
     * @param url URL контента
     * @return Optional с контентом, если он найден в кэше и не истек
     */
    Optional<MediaContent> get(String url);
    
    /**
     * Сохраняет контент в кэш.
     *
     * @param url URL контента
     * @param content медиа контент для сохранения
     */
    void put(String url, MediaContent content);
    
    /**
     * Удаляет из кэша все истекшие записи.
     * Обычно вызывается по расписанию.
     */
    void evictExpired();
}
