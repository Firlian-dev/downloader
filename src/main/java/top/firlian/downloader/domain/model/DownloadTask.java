package top.firlian.downloader.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Задача загрузки медиа контента.
 * Отслеживает состояние процесса загрузки для конкретного URL.
 */
@Value
@Builder
public class DownloadTask {
    /** URL для загрузки */
    String url;
    
    /** Провайдер контента */
    Provider provider;
    
    /** ID чата Telegram, запросившего загрузку */
    Long chatId;
    
    /** Текущий статус задачи */
    TaskStatus status;
    
    /** Сообщение об ошибке (если статус FAILED) */
    String errorMessage;
}
