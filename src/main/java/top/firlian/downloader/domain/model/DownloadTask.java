package top.firlian.downloader.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DownloadTask {
    String url;
    Provider provider;
    Long chatId;
    TaskStatus status;
    String errorMessage;
}
