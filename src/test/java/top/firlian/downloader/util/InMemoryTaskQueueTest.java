package top.firlian.downloader.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.firlian.downloader.domain.model.DownloadTask;
import top.firlian.downloader.domain.model.Provider;
import top.firlian.downloader.domain.model.TaskStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskQueueTest {

    private InMemoryTaskQueue taskQueue;

    @BeforeEach
    void setUp() {
        taskQueue = new InMemoryTaskQueue();
    }

    @Test
    void testAddTask() {
        DownloadTask task = DownloadTask.builder()
                .url("https://example.com/video")
                .provider(Provider.YOUTUBE)
                .chatId(12345L)
                .status(TaskStatus.PENDING)
                .build();

        assertTrue(taskQueue.addTask(task));
        assertFalse(taskQueue.addTask(task)); // Duplicate should fail
    }

    @Test
    void testGetTask() {
        String url = "https://example.com/video";
        DownloadTask task = DownloadTask.builder()
                .url(url)
                .provider(Provider.YOUTUBE)
                .chatId(12345L)
                .status(TaskStatus.PENDING)
                .build();

        taskQueue.addTask(task);

        Optional<DownloadTask> retrieved = taskQueue.getTask(url);
        assertTrue(retrieved.isPresent());
        assertEquals(url, retrieved.get().getUrl());
    }

    @Test
    void testCompleteTask() {
        String url = "https://example.com/video";
        DownloadTask task = DownloadTask.builder()
                .url(url)
                .provider(Provider.YOUTUBE)
                .chatId(12345L)
                .status(TaskStatus.PENDING)
                .build();

        taskQueue.addTask(task);
        taskQueue.completeTask(url);

        Optional<DownloadTask> retrieved = taskQueue.getTask(url);
        assertTrue(retrieved.isPresent());
        assertEquals(TaskStatus.COMPLETED, retrieved.get().getStatus());
    }

    @Test
    void testFailTask() {
        String url = "https://example.com/video";
        String errorMsg = "Download failed";
        DownloadTask task = DownloadTask.builder()
                .url(url)
                .provider(Provider.YOUTUBE)
                .chatId(12345L)
                .status(TaskStatus.PENDING)
                .build();

        taskQueue.addTask(task);
        taskQueue.failTask(url, errorMsg);

        Optional<DownloadTask> retrieved = taskQueue.getTask(url);
        assertTrue(retrieved.isPresent());
        assertEquals(TaskStatus.FAILED, retrieved.get().getStatus());
        assertEquals(errorMsg, retrieved.get().getErrorMessage());
    }
}
