package top.firlian.downloader.domain.error;

public class UnsupportedProviderException extends RuntimeException {
    public UnsupportedProviderException(String message) {
        super(message);
    }
}
