package top.firlian.downloader.domain.error;

/**
 * Исключение, выбрасываемое когда провайдер не поддерживается.
 * Возникает при попытке загрузки с неизвестного или неподдерживаемого источника.
 */
public class UnsupportedProviderException extends RuntimeException {
    /**
     * Создает новое исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public UnsupportedProviderException(String message) {
        super(message);
    }
}
