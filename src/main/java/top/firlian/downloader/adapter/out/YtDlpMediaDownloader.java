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

/**
 * Реализация загрузчика медиа контента с использованием утилиты yt-dlp.
 * yt-dlp - это универсальный инструмент для загрузки видео и аудио
 * с различных платформ (YouTube, VK, Instagram и др.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YtDlpMediaDownloader implements MediaDownloader {

    /** Маппер для парсинга JSON метаданных */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Путь к исполняемому файлу yt-dlp */
    @Value("${downloader.ytdlp.bin:/usr/local/bin/yt-dlp}")
    private String ytdlpBin;

    /** Директория для сохранения загруженных файлов */
    @Value("${downloader.download-dir:/tmp/downloads}")
    private String downloadDir;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<MediaContent> download(String url) {
        return Mono.fromCallable(() -> {
            try {
                // Создаем директорию для загрузок, если её нет
                Path downloadPath = Paths.get(downloadDir);
                if (!Files.exists(downloadPath)) {
                    Files.createDirectories(downloadPath);
                }

                // Сначала получаем метаданные для проверки наличия нескольких элементов
                JsonNode metadata = getMetadata(url);
                
                if (metadata.has("entries") && metadata.get("entries").isArray()) {
                    // Обрабатываем несколько элементов (плейлист/карусель)
                    return handleMultipleItems(url, metadata);
                } else {
                    // Загружаем одиночный элемент
                    return downloadSingleItem(url, metadata, 0);
                }
            } catch (Exception e) {
                log.error("Ошибка загрузки с URL: {}", url, e);
                // Проверяем специфичные ошибки доступа к контенту
                if (e.getMessage().contains("Private") || e.getMessage().contains("unavailable")) {
                    throw new ContentUnavailableException("Контент недоступен", e);
                }
                throw new DownloadException("Ошибка загрузки. Попробуйте позже", e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<MediaContent> downloadSpecificItem(String url, int itemIndex) {
        return Mono.fromCallable(() -> {
            try {
                JsonNode metadata = getMetadata(url);
                return downloadSingleItem(url, metadata, itemIndex);
            } catch (Exception e) {
                log.error("Ошибка загрузки элемента {} с URL: {}", itemIndex, url, e);
                throw new DownloadException("Ошибка загрузки. Попробуйте позже", e);
            }
        });
    }

    /**
     * Получает метаданные контента без загрузки файла.
     * Использует опцию --dump-json для получения информации о медиа.
     *
     * @param url URL контента
     * @return JSON объект с метаданными
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws InterruptedException если процесс был прерван
     */
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
            log.error("Ошибка извлечения метаданных yt-dlp с кодом: {}, вывод: {}", exitCode, output);
            throw new DownloadException("Не удалось извлечь метаданные");
        }

        return objectMapper.readTree(output.toString());
    }

    /**
     * Обрабатывает контент с несколькими элементами (плейлист, карусель).
     * Создает список элементов и загружает первый из них по умолчанию.
     *
     * @param url URL контента
     * @param metadata метаданные с информацией о всех элементах
     * @return медиа контент с первым загруженным элементом и списком всех доступных
     */
    private MediaContent handleMultipleItems(String url, JsonNode metadata) {
        List<MediaItem> items = new ArrayList<>();
        JsonNode entries = metadata.get("entries");
        
        // Создаем список всех доступных элементов
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

        // Загружаем первый элемент по умолчанию
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

    /**
     * Загружает одиночный медиа файл.
     *
     * @param url URL для загрузки
     * @param metadata метаданные элемента
     * @param itemIndex индекс элемента (0 для одиночного файла)
     * @return загруженный медиа контент
     */
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
            
            // Если указан индекс элемента, добавляем соответствующую опцию
            if (itemIndex > 0) {
                pb.command().add("--playlist-items");
                pb.command().add(String.valueOf(itemIndex + 1));
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            String downloadedFile = null;
            
            // Читаем вывод процесса и пытаемся извлечь путь к загруженному файлу
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Вывод yt-dlp: {}", line);
                    output.append(line).append("\n");
                    
                    // Извлекаем путь к файлу из вывода yt-dlp
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
                log.error("Ошибка загрузки yt-dlp с кодом: {}, вывод: {}", exitCode, output);
                throw new DownloadException("Загрузка не удалась");
            }

            // Если не удалось извлечь путь из вывода, ищем последний измененный файл
            if (downloadedFile == null) {
                File dir = new File(downloadDir);
                File[] files = dir.listFiles((d, name) -> !name.startsWith("."));
                if (files != null && files.length > 0) {
                    // Находим самый свежий файл
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
                throw new DownloadException("Не удалось определить путь к загруженному файлу");
            }

            File file = new File(downloadedFile);
            long fileSize = file.length();

            log.info("Файл успешно загружен: {}, размер: {} байт", downloadedFile, fileSize);

            return MediaContent.builder()
                    .url(url)
                    .type(determineMediaType(metadata))
                    .title(metadata.has("title") ? metadata.get("title").asText() : "Media")
                    .sizeBytes(fileSize)
                    .filePath(downloadedFile)
                    .items(null)
                    .build();

        } catch (IOException | InterruptedException e) {
            log.error("Ошибка во время загрузки", e);
            throw new DownloadException("Загрузка не удалась", e);
        }
    }

    /**
     * Определяет тип медиа на основе метаданных.
     * Проверяет наличие видео/аудио кодеков и расширение файла.
     *
     * @param metadata метаданные файла
     * @return тип медиа (VIDEO, AUDIO, PHOTO или DOCUMENT)
     */
    private MediaType determineMediaType(JsonNode metadata) {
        // Проверяем наличие видео кодека
        if (metadata.has("vcodec") && !metadata.get("vcodec").asText().equals("none")) {
            return MediaType.VIDEO;
        }
        // Проверяем наличие аудио кодека
        if (metadata.has("acodec") && !metadata.get("acodec").asText().equals("none")) {
            return MediaType.AUDIO;
        }
        // Определяем по расширению файла
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
