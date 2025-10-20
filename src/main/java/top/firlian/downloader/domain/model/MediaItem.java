package top.firlian.downloader.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MediaItem {
    int index;
    String url;
    MediaType type;
    String title;
    long sizeBytes;
}
