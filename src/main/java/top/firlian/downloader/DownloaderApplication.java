package top.firlian.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс Spring Boot приложения для загрузки медиа контента.
 * Приложение предоставляет Telegram бота для загрузки видео, фото и других
 * медиа файлов с различных платформ (YouTube, VK, Instagram).
 */
@SpringBootApplication
@EnableScheduling
public class DownloaderApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(DownloaderApplication.class, args);
    }
}
