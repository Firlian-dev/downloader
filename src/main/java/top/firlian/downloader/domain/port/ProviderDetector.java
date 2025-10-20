package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.Provider;

public interface ProviderDetector {
    Provider detectProvider(String url);
}
