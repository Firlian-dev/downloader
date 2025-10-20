package top.firlian.downloader.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import top.firlian.downloader.domain.error.ContentUnavailableException;
import top.firlian.downloader.domain.error.DownloadException;
import top.firlian.downloader.domain.model.MediaContent;
import top.firlian.downloader.domain.model.MediaItem;
import top.firlian.downloader.domain.model.MediaType;
import top.firlian.downloader.domain.port.MediaDownloader;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client implementation for yt-dlp service.
 * Communicates with a containerized yt-dlp service via REST API.
 */
@Slf4j
@Component("httpYtDlpMediaDownloader")
@RequiredArgsConstructor
public class HttpYtDlpMediaDownloader implements MediaDownloader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient.Builder webClientBuilder;

    @Value("${downloader.ytdlp.service-url:http://localhost:8090}")
    private String serviceUrl;

    @Value("${downloader.download-dir:/tmp/downloads}")
    private String downloadDir;

    private WebClient getWebClient() {
        return webClientBuilder
                .baseUrl(serviceUrl)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<MediaContent> download(String url) {
        return getMetadata(url)
                .flatMap(metadata -> {
                    try {
                        if (metadata.has("entries") && metadata.get("entries").isArray()) {
                            // Handle multiple items (playlist/carousel)
                            return handleMultipleItems(url, metadata);
                        } else {
                            // Download single item
                            return downloadSingleItem(url, metadata, 0);
                        }
                    } catch (Exception e) {
                        log.error("Error processing download for URL: {}", url, e);
                        if (e.getMessage().contains("Private") || e.getMessage().contains("unavailable")) {
                            return Mono.error(new ContentUnavailableException("Контент недоступен", e));
                        }
                        return Mono.error(new DownloadException("Ошибка загрузки. Попробуйте позже", e));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error downloading from URL: {}", url, e);
                    if (e instanceof ContentUnavailableException) {
                        return Mono.error(e);
                    }
                    return Mono.error(new DownloadException("Ошибка загрузки. Попробуйте позже", e));
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<MediaContent> downloadSpecificItem(String url, int itemIndex) {
        return getMetadata(url)
                .flatMap(metadata -> downloadSingleItem(url, metadata, itemIndex))
                .onErrorResume(e -> {
                    log.error("Error downloading item {} from URL: {}", itemIndex, url, e);
                    return Mono.error(new DownloadException("Ошибка загрузки. Попробуйте позже", e));
                });
    }

    /**
     * Get metadata from yt-dlp service
     */
    private Mono<JsonNode> getMetadata(String url) {
        Map<String, String> request = new HashMap<>();
        request.put("url", url);

        return getWebClient()
                .post()
                .uri("/metadata")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .flatMap(response -> {
                    try {
                        return Mono.just(objectMapper.readTree(response));
                    } catch (Exception e) {
                        log.error("Error parsing metadata JSON", e);
                        return Mono.error(new DownloadException("Не удалось извлечь метаданные"));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error getting metadata for URL: {}", url, e);
                    return Mono.error(new DownloadException("Не удалось извлечь метаданные", e));
                });
    }

    /**
     * Handle multiple items (playlist/carousel)
     */
    private Mono<MediaContent> handleMultipleItems(String url, JsonNode metadata) {
        List<MediaItem> items = new ArrayList<>();
        JsonNode entries = metadata.get("entries");

        // Create list of all available items
        for (int i = 0; i < entries.size(); i++) {
            JsonNode entry = entries.get(i);
            items.add(MediaItem.builder()
                    .index(i)
                    .url(entry.has("url") ? entry.get("url").asText() : url)
                    .type(determineMediaType(entry))
                    .title(entry.has("title") ? entry.get("title").asText() : "Item " + (i + 1))
                    .sizeBytes(entry.has("filesize") ? entry.get("filesize").asLong() : 0)
                    .build());
        }

        // Download first item by default
        return downloadSingleItem(url, entries.get(0), 0)
                .map(firstItem -> MediaContent.builder()
                        .url(url)
                        .type(firstItem.getType())
                        .title(metadata.has("title") ? metadata.get("title").asText() : "Media")
                        .sizeBytes(firstItem.getSizeBytes())
                        .filePath(firstItem.getFilePath())
                        .items(items)
                        .build());
    }

    /**
     * Download a single item via HTTP service
     */
    private Mono<MediaContent> downloadSingleItem(String url, JsonNode metadata, int itemIndex) {
        Map<String, Object> request = new HashMap<>();
        request.put("url", url);
        if (itemIndex > 0) {
            request.put("itemIndex", itemIndex);
        }

        return getWebClient()
                .post()
                .uri("/download")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DownloadResponse.class)
                .timeout(Duration.ofMinutes(5))
                .map(response -> {
                    // Verify file exists
                    File file = new File(response.getFilePath());
                    if (!file.exists()) {
                        throw new DownloadException("Файл не найден после загрузки: " + response.getFilePath());
                    }

                    log.info("File successfully downloaded via HTTP service: {}, size: {} bytes",
                            response.getFilePath(), response.getSizeBytes());

                    return MediaContent.builder()
                            .url(url)
                            .type(determineMediaType(metadata))
                            .title(metadata.has("title") ? metadata.get("title").asText() : "Media")
                            .sizeBytes(response.getSizeBytes())
                            .filePath(response.getFilePath())
                            .items(null)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error during download", e);
                    return Mono.error(new DownloadException("Загрузка не удалась", e));
                });
    }

    /**
     * Determine media type from metadata
     */
    private MediaType determineMediaType(JsonNode metadata) {
        // Check for video codec
        if (metadata.has("vcodec") && !metadata.get("vcodec").asText().equals("none")) {
            return MediaType.VIDEO;
        }
        // Check for audio codec
        if (metadata.has("acodec") && !metadata.get("acodec").asText().equals("none")) {
            return MediaType.AUDIO;
        }
        // Determine by file extension
        if (metadata.has("ext")) {
            String ext = metadata.get("ext").asText().toLowerCase();
            if (ext.matches("mp4|webm|mkv|avi|mov")) {
                return MediaType.VIDEO;
            }
            if (ext.matches("jpg|jpeg|png|gif|webp")) {
                return MediaType.PHOTO;
            }
            if (ext.matches("mp3|m4a|opus|ogg|wav")) {
                return MediaType.AUDIO;
            }
        }
        return MediaType.DOCUMENT;
    }

    /**
     * DTO for download response from yt-dlp service
     */
    private static class DownloadResponse {
        private String filePath;
        private String fileName;
        private Long sizeBytes;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }
    }
}
