package top.firlian.downloader.domain.error;

/**
 * Исключение, выбрасываемое когда контент недоступен для загрузки.
 * Например, когда контент приватный, удален или заблокирован.
 */
public class ContentUnavailableException extends RuntimeException {
    /**
     * Создает новое исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public ContentUnavailableException(String message) {
        super(message);
    }

    /**
     * Создает новое исключение с указанным сообщением и причиной.
     *
     * @param message сообщение об ошибке
     * @param cause причина возникновения исключения
     */
    public ContentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
