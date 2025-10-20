package top.firlian.downloader.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MediaContent {
    String url;
    MediaType type;
    String title;
    long sizeBytes;
    String filePath;
    List<MediaItem> items;
}
