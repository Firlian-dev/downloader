package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.MediaContent;
import reactor.core.publisher.Mono;

/**
 * Интерфейс для загрузки медиа контента.
 * Определяет контракт для загрузки медиа файлов с различных источников.
 */
public interface MediaDownloader {
    /**
     * Загружает медиа контент по указанному URL.
     * Если URL содержит несколько элементов (плейлист, карусель),
     * загружается первый элемент, а информация обо всех элементах
     * включается в результат.
     *
     * @param url URL для загрузки
     * @return Mono с загруженным контентом
     */
    Mono<MediaContent> download(String url);
    
    /**
     * Загружает конкретный элемент из плейлиста или карусели.
     *
     * @param url URL плейлиста или карусели
     * @param itemIndex индекс элемента для загрузки (начиная с 0)
     * @return Mono с загруженным элементом
     */
    Mono<MediaContent> downloadSpecificItem(String url, int itemIndex);
}
