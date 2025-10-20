package top.firlian.downloader.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.model.DownloadTask;
import top.firlian.downloader.domain.model.TaskStatus;
import top.firlian.downloader.domain.port.TaskQueue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация очереди задач загрузки.
 * Хранит задачи в памяти и управляет их состоянием.
 * Потокобезопасная реализация с использованием ConcurrentHashMap.
 */
@Slf4j
@Component
public class InMemoryTaskQueue implements TaskQueue {

    /** Хранилище задач с URL в качестве ключа */
    private final Map<String, DownloadTask> tasks = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addTask(DownloadTask task) {
        if (tasks.containsKey(task.getUrl())) {
            log.warn("Задача уже существует для URL: {}", task.getUrl());
            return false;
        }
        tasks.put(task.getUrl(), task);
        log.info("Добавлена задача для URL: {}", task.getUrl());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DownloadTask> getTask(String url) {
        return Optional.ofNullable(tasks.get(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void completeTask(String url) {
        DownloadTask task = tasks.get(url);
        if (task != null) {
            // Обновляем статус задачи на COMPLETED
            tasks.put(url, DownloadTask.builder()
                    .url(task.getUrl())
                    .provider(task.getProvider())
                    .chatId(task.getChatId())
                    .status(TaskStatus.COMPLETED)
                    .build());
            log.info("Задача завершена для URL: {}", url);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void failTask(String url, String errorMessage) {
        DownloadTask task = tasks.get(url);
        if (task != null) {
            // Обновляем статус задачи на FAILED с сообщением об ошибке
            tasks.put(url, DownloadTask.builder()
                    .url(task.getUrl())
                    .provider(task.getProvider())
                    .chatId(task.getChatId())
                    .status(TaskStatus.FAILED)
                    .errorMessage(errorMessage)
                    .build());
            log.error("Задача провалена для URL: {} с ошибкой: {}", url, errorMessage);
        }
    }
}
