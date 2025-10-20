package top.firlian.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DownloaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(DownloaderApplication.class, args);
    }
}
