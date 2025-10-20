package top.firlian.downloader.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import top.firlian.downloader.adapter.out.HttpYtDlpMediaDownloader;
import top.firlian.downloader.adapter.out.YtDlpMediaDownloader;
import top.firlian.downloader.domain.port.MediaDownloader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MediaDownloaderConfig to verify correct selection of downloader implementation.
 */
@SpringBootTest
class MediaDownloaderConfigTest {

    @Autowired
    private MediaDownloader mediaDownloader;

    @Test
    void contextLoads() {
        assertNotNull(mediaDownloader);
    }

    @SpringBootTest
    @TestPropertySource(properties = {"downloader.ytdlp.mode=local"})
    static class LocalModeTest {

        @Autowired
        private MediaDownloader mediaDownloader;

        @Test
        void shouldUseLocalDownloader() {
            assertNotNull(mediaDownloader);
            assertInstanceOf(YtDlpMediaDownloader.class, mediaDownloader);
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {"downloader.ytdlp.mode=http"})
    static class HttpModeTest {

        @Autowired
        private MediaDownloader mediaDownloader;

        @Test
        void shouldUseHttpDownloader() {
            assertNotNull(mediaDownloader);
            assertInstanceOf(HttpYtDlpMediaDownloader.class, mediaDownloader);
        }
    }
}
