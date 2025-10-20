package top.firlian.downloader.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.port.CacheRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {

    private final CacheRepository cacheRepository;

    @Scheduled(fixedRateString = "${downloader.cache-eviction-interval-ms:3600000}")
    public void evictExpiredCache() {
        log.info("Running scheduled cache eviction");
        cacheRepository.evictExpired();
    }
}
