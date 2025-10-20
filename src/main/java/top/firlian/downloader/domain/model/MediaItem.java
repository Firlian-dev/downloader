package top.firlian.downloader.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Элемент медиа контента из плейлиста или карусели.
 * Представляет отдельный медиа файл в коллекции элементов.
 */
@Value
@Builder
public class MediaItem {
    /** Индекс элемента в коллекции (начиная с 0) */
    int index;
    
    /** URL элемента */
    String url;
    
    /** Тип медиа контента */
    MediaType type;
    
    /** Название элемента */
    String title;
    
    /** Размер файла в байтах */
    long sizeBytes;
}
