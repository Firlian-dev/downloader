package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.MediaContent;
import reactor.core.publisher.Mono;

public interface MediaDownloader {
    Mono<MediaContent> download(String url);
    Mono<MediaContent> downloadSpecificItem(String url, int itemIndex);
}
