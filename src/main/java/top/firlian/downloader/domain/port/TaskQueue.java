package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.DownloadTask;

import java.util.Optional;

/**
 * Интерфейс очереди задач загрузки.
 * Управляет задачами загрузки медиа контента, отслеживая их состояние.
 */
public interface TaskQueue {
    /**
     * Добавляет новую задачу в очередь.
     *
     * @param task задача для добавления
     * @return true если задача успешно добавлена, false если задача с таким URL уже существует
     */
    boolean addTask(DownloadTask task);
    
    /**
     * Получает задачу по URL.
     *
     * @param url URL задачи
     * @return Optional с задачей, если она найдена
     */
    Optional<DownloadTask> getTask(String url);
    
    /**
     * Отмечает задачу как успешно завершенную.
     *
     * @param url URL задачи
     */
    void completeTask(String url);
    
    /**
     * Отмечает задачу как завершенную с ошибкой.
     *
     * @param url URL задачи
     * @param errorMessage сообщение об ошибке
     */
    void failTask(String url, String errorMessage);
}
