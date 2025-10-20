package top.firlian.downloader.util;

import org.junit.jupiter.api.Test;
import top.firlian.downloader.domain.model.Provider;

import static org.junit.jupiter.api.Assertions.*;

class UrlProviderDetectorTest {

    private final UrlProviderDetector detector = new UrlProviderDetector();

    @Test
    void testDetectYouTube() {
        assertEquals(Provider.YOUTUBE, detector.detectProvider("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertEquals(Provider.YOUTUBE, detector.detectProvider("https://youtu.be/dQw4w9WgXcQ"));
        assertEquals(Provider.YOUTUBE, detector.detectProvider("https://www.youtube.com/shorts/abc123"));
        assertEquals(Provider.YOUTUBE, detector.detectProvider("http://youtube.com/watch?v=test"));
    }

    @Test
    void testDetectVK() {
        assertEquals(Provider.VK, detector.detectProvider("https://vk.com/wall-12345_67890"));
        assertEquals(Provider.VK, detector.detectProvider("https://www.vk.com/video12345_67890"));
        assertEquals(Provider.VK, detector.detectProvider("https://vk.ru/wall123"));
    }

    @Test
    void testDetectInstagram() {
        assertEquals(Provider.INSTAGRAM, detector.detectProvider("https://www.instagram.com/p/ABC123/"));
        assertEquals(Provider.INSTAGRAM, detector.detectProvider("https://instagram.com/reel/XYZ789/"));
        assertEquals(Provider.INSTAGRAM, detector.detectProvider("https://instagr.am/p/test/"));
    }

    @Test
    void testDetectUnknown() {
        assertEquals(Provider.UNKNOWN, detector.detectProvider("https://example.com"));
        assertEquals(Provider.UNKNOWN, detector.detectProvider("not a url"));
        assertEquals(Provider.UNKNOWN, detector.detectProvider(""));
        assertEquals(Provider.UNKNOWN, detector.detectProvider(null));
    }
}
