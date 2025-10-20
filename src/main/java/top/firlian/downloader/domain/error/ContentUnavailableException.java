package top.firlian.downloader.domain.error;

public class ContentUnavailableException extends RuntimeException {
    public ContentUnavailableException(String message) {
        super(message);
    }

    public ContentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
