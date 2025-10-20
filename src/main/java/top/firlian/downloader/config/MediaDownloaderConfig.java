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
 * Configuration class for selecting the appropriate MediaDownloader implementation
 * based on the ytdlp.mode setting.
 */
@Slf4j
@Configuration
public class MediaDownloaderConfig {

    @Value("${downloader.ytdlp.mode:local}")
    private String ytdlpMode;

    /**
     * Provides the primary MediaDownloader bean based on configuration.
     * 
     * @param localDownloader Local binary implementation
     * @param httpDownloader HTTP service implementation
     * @return The selected MediaDownloader implementation
     */
    @Bean
    @Primary
    public MediaDownloader mediaDownloader(
            YtDlpMediaDownloader localDownloader,
            HttpYtDlpMediaDownloader httpDownloader) {
        
        if ("http".equalsIgnoreCase(ytdlpMode)) {
            log.info("Using HTTP yt-dlp service mode");
            return httpDownloader;
        } else {
            log.info("Using local yt-dlp binary mode");
            return localDownloader;
        }
    }
}
