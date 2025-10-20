package top.firlian.downloader.domain.port;

import top.firlian.downloader.domain.model.Provider;

/**
 * Интерфейс для определения провайдера медиа контента по URL.
 * Анализирует URL и определяет, с какого источника будет производиться загрузка.
 */
public interface ProviderDetector {
    /**
     * Определяет провайдера медиа контента по URL.
     *
     * @param url URL для анализа
     * @return провайдер контента или UNKNOWN, если провайдер не поддерживается
     */
    Provider detectProvider(String url);
}
