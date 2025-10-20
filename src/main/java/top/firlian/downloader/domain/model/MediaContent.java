package top.firlian.downloader.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Загруженный медиа контент.
 * Содержит информацию о загруженном файле и его метаданных.
 * Может содержать коллекцию элементов для плейлистов и каруселей.
 */
@Value
@Builder
public class MediaContent {
    /** URL источника контента */
    String url;
    
    /** Тип медиа контента */
    MediaType type;
    
    /** Название контента */
    String title;
    
    /** Размер файла в байтах */
    long sizeBytes;
    
    /** Путь к загруженному файлу в файловой системе */
    String filePath;
    
    /** Список элементов для плейлистов/каруселей (null для одиночных файлов) */
    List<MediaItem> items;
}
