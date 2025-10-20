package top.firlian.downloader.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import top.firlian.downloader.domain.error.ContentUnavailableException;
import top.firlian.downloader.domain.error.DownloadException;
import top.firlian.downloader.domain.model.MediaContent;
import top.firlian.downloader.domain.model.MediaItem;
import top.firlian.downloader.domain.model.MediaType;
import top.firlian.downloader.domain.port.MediaDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YtDlpMediaDownloader implements MediaDownloader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${downloader.ytdlp.bin:/usr/local/bin/yt-dlp}")
    private String ytdlpBin;

    @Value("${downloader.download-dir:/tmp/downloads}")
    private String downloadDir;

    @Override
    public Mono<MediaContent> download(String url) {
        return Mono.fromCallable(() -> {
            try {
                // Create download directory if it doesn't exist
                Path downloadPath = Paths.get(downloadDir);
                if (!Files.exists(downloadPath)) {
                    Files.createDirectories(downloadPath);
                }

                // First, get metadata to check if there are multiple items
                JsonNode metadata = getMetadata(url);
                
                if (metadata.has("entries") && metadata.get("entries").isArray()) {
                    // Multiple items (playlist/carousel)
                    return handleMultipleItems(url, metadata);
                } else {
                    // Single item
                    return downloadSingleItem(url, metadata, 0);
                }
            } catch (Exception e) {
                log.error("Error downloading from URL: {}", url, e);
                if (e.getMessage().contains("Private") || e.getMessage().contains("unavailable")) {
                    throw new ContentUnavailableException("Контент недоступен", e);
                }
                throw new DownloadException("Ошибка загрузки. Попробуйте позже", e);
            }
        });
    }

    @Override
    public Mono<MediaContent> downloadSpecificItem(String url, int itemIndex) {
        return Mono.fromCallable(() -> {
            try {
                JsonNode metadata = getMetadata(url);
                return downloadSingleItem(url, metadata, itemIndex);
            } catch (Exception e) {
                log.error("Error downloading item {} from URL: {}", itemIndex, url, e);
                throw new DownloadException("Ошибка загрузки. Попробуйте позже", e);
            }
        });
    }

    private JsonNode getMetadata(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                ytdlpBin,
                "--dump-json",
                "--no-warnings",
                url
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("yt-dlp metadata extraction failed with exit code: {}, output: {}", exitCode, output);
            throw new DownloadException("Failed to extract metadata");
        }

        return objectMapper.readTree(output.toString());
    }

    private MediaContent handleMultipleItems(String url, JsonNode metadata) {
        List<MediaItem> items = new ArrayList<>();
        JsonNode entries = metadata.get("entries");
        
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
        MediaContent firstItem = downloadSingleItem(url, entries.get(0), 0);

        return MediaContent.builder()
                .url(url)
                .type(firstItem.getType())
                .title(metadata.has("title") ? metadata.get("title").asText() : "Media")
                .sizeBytes(firstItem.getSizeBytes())
                .filePath(firstItem.getFilePath())
                .items(items)
                .build();
    }

    private MediaContent downloadSingleItem(String url, JsonNode metadata, int itemIndex) {
        try {
            String outputTemplate = downloadDir + "/%(title)s-%(id)s.%(ext)s";
            
            ProcessBuilder pb = new ProcessBuilder(
                    ytdlpBin,
                    "--no-warnings",
                    "--no-playlist",
                    "-o", outputTemplate,
                    url
            );
            
            if (itemIndex > 0) {
                pb.command().add("--playlist-items");
                pb.command().add(String.valueOf(itemIndex + 1));
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            String downloadedFile = null;
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("yt-dlp output: {}", line);
                    output.append(line).append("\n");
                    
                    // Try to extract the downloaded file path
                    if (line.contains("Destination:") || line.contains("has already been downloaded")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            downloadedFile = parts[1].trim();
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("yt-dlp download failed with exit code: {}, output: {}", exitCode, output);
                throw new DownloadException("Download failed");
            }

            // If we couldn't extract the file path from output, try to find it
            if (downloadedFile == null) {
                File dir = new File(downloadDir);
                File[] files = dir.listFiles((d, name) -> !name.startsWith("."));
                if (files != null && files.length > 0) {
                    // Get the most recently modified file
                    File latest = files[0];
                    for (File f : files) {
                        if (f.lastModified() > latest.lastModified()) {
                            latest = f;
                        }
                    }
                    downloadedFile = latest.getAbsolutePath();
                }
            }

            if (downloadedFile == null) {
                throw new DownloadException("Could not determine downloaded file path");
            }

            File file = new File(downloadedFile);
            long fileSize = file.length();

            log.info("Successfully downloaded file: {}, size: {} bytes", downloadedFile, fileSize);

            return MediaContent.builder()
                    .url(url)
                    .type(determineMediaType(metadata))
                    .title(metadata.has("title") ? metadata.get("title").asText() : "Media")
                    .sizeBytes(fileSize)
                    .filePath(downloadedFile)
                    .items(null)
                    .build();

        } catch (IOException | InterruptedException e) {
            log.error("Error during download", e);
            throw new DownloadException("Download failed", e);
        }
    }

    private MediaType determineMediaType(JsonNode metadata) {
        if (metadata.has("vcodec") && !metadata.get("vcodec").asText().equals("none")) {
            return MediaType.VIDEO;
        }
        if (metadata.has("acodec") && !metadata.get("acodec").asText().equals("none")) {
            return MediaType.AUDIO;
        }
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
}
