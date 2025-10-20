package top.firlian.downloader.domain.error;

/**
 * Исключение, выбрасываемое при ошибках загрузки медиа контента.
 * Базовое исключение для всех ошибок, связанных с загрузкой файлов.
 */
public class DownloadException extends RuntimeException {
    /**
     * Создает новое исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public DownloadException(String message) {
        super(message);
    }

    /**
     * Создает новое исключение с указанным сообщением и причиной.
     *
     * @param message сообщение об ошибке
     * @param cause причина возникновения исключения
     */
    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
