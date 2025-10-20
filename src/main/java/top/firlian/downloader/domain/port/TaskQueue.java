package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.DownloadTask;

import java.util.Optional;

public interface TaskQueue {
    boolean addTask(DownloadTask task);
    Optional<DownloadTask> getTask(String url);
    void completeTask(String url);
    void failTask(String url, String errorMessage);
}
