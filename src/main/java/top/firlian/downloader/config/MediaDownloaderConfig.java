package top.firlian.downloader.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.firlian.downloader.adapter.out.HttpYtDlpMediaDownloader;
import top.firlian.downloader.adapter.out.YtDlpMediaDownloader;
import top.firlian.downloader.domain.port.MediaDownloader;

/**
 * Класс конфигурации для выбора подходящей реализации MediaDownloader
 * на основе настройки ytdlp.mode.
 */
@Slf4j
@Configuration
public class MediaDownloaderConfig {

    @Value("${downloader.ytdlp.mode:local}")
    private String ytdlpMode;

    /**
     * Предоставляет основной MediaDownloader bean на основе конфигурации.
     * 
     * @param localDownloader Реализация с локальным бинарным файлом
     * @param httpDownloader Реализация HTTP сервиса
     * @return Выбранная реализация MediaDownloader
     */
    @Bean
    @Primary
    public MediaDownloader mediaDownloader(
            YtDlpMediaDownloader localDownloader,
            HttpYtDlpMediaDownloader httpDownloader) {
        
        if ("http".equalsIgnoreCase(ytdlpMode)) {
            log.info("Используется режим HTTP сервиса yt-dlp");
            return httpDownloader;
        } else {
            log.info("Используется режим локального бинарного файла yt-dlp");
            return localDownloader;
        }
    }
}
