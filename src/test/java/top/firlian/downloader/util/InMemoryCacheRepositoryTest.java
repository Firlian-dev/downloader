package top.firlian.downloader.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.firlian.downloader.domain.model.MediaContent;
import top.firlian.downloader.domain.model.MediaType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCacheRepositoryTest {

    private InMemoryCacheRepository cacheRepository;

    @BeforeEach
    void setUp() {
        cacheRepository = new InMemoryCacheRepository(24);
    }

    @Test
    void testPutAndGet() {
        String url = "https://example.com/video";
        MediaContent content = MediaContent.builder()
                .url(url)
                .type(MediaType.VIDEO)
                .title("Test Video")
                .sizeBytes(1000)
                .filePath("/tmp/test.mp4")
                .build();

        cacheRepository.put(url, content);

        Optional<MediaContent> retrieved = cacheRepository.get(url);
        assertTrue(retrieved.isPresent());
        assertEquals(content, retrieved.get());
    }

    @Test
    void testGetNonExistent() {
        Optional<MediaContent> retrieved = cacheRepository.get("https://nonexistent.com");
        assertFalse(retrieved.isPresent());
    }
}
