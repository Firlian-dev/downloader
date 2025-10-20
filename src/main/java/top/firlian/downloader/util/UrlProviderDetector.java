package top.firlian.downloader.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.model.Provider;
import top.firlian.downloader.domain.port.ProviderDetector;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlProviderDetector implements ProviderDetector {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|youtube\\.com/shorts).*"
    );

    private static final Pattern VK_PATTERN = Pattern.compile(
            "(https?://)?(www\\.)?(vk\\.com|vk\\.ru).*"
    );

    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            "(https?://)?(www\\.)?(instagram\\.com|instagr\\.am).*"
    );

    @Override
    public Provider detectProvider(String url) {
        if (url == null || url.isBlank()) {
            return Provider.UNKNOWN;
        }

        String normalizedUrl = url.trim().toLowerCase();

        if (YOUTUBE_PATTERN.matcher(normalizedUrl).matches()) {
            log.debug("Detected YouTube provider for URL: {}", url);
            return Provider.YOUTUBE;
        }

        if (VK_PATTERN.matcher(normalizedUrl).matches()) {
            log.debug("Detected VK provider for URL: {}", url);
            return Provider.VK;
        }

        if (INSTAGRAM_PATTERN.matcher(normalizedUrl).matches()) {
            log.debug("Detected Instagram provider for URL: {}", url);
            return Provider.INSTAGRAM;
        }

        log.warn("Unknown provider for URL: {}", url);
        return Provider.UNKNOWN;
    }
}
