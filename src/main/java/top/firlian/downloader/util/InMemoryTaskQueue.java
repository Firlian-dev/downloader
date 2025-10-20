package top.firlian.downloader.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.model.DownloadTask;
import top.firlian.downloader.domain.model.TaskStatus;
import top.firlian.downloader.domain.port.TaskQueue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InMemoryTaskQueue implements TaskQueue {

    private final Map<String, DownloadTask> tasks = new ConcurrentHashMap<>();

    @Override
    public boolean addTask(DownloadTask task) {
        if (tasks.containsKey(task.getUrl())) {
            log.warn("Task already exists for URL: {}", task.getUrl());
            return false;
        }
        tasks.put(task.getUrl(), task);
        log.info("Added task for URL: {}", task.getUrl());
        return true;
    }

    @Override
    public Optional<DownloadTask> getTask(String url) {
        return Optional.ofNullable(tasks.get(url));
    }

    @Override
    public void completeTask(String url) {
        DownloadTask task = tasks.get(url);
        if (task != null) {
            tasks.put(url, DownloadTask.builder()
                    .url(task.getUrl())
                    .provider(task.getProvider())
                    .chatId(task.getChatId())
                    .status(TaskStatus.COMPLETED)
                    .build());
            log.info("Completed task for URL: {}", url);
        }
    }

    @Override
    public void failTask(String url, String errorMessage) {
        DownloadTask task = tasks.get(url);
        if (task != null) {
            tasks.put(url, DownloadTask.builder()
                    .url(task.getUrl())
                    .provider(task.getProvider())
                    .chatId(task.getChatId())
                    .status(TaskStatus.FAILED)
                    .errorMessage(errorMessage)
                    .build());
            log.error("Failed task for URL: {} with error: {}", url, errorMessage);
        }
    }
}
