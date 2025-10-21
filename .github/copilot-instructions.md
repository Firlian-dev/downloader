# Правила кодирования для GitHub Copilot

## Язык документации и комментариев

**ВСЯ документация, комментарии и Javadoc ДОЛЖНЫ быть на русском языке.**

### Примеры:

```java
/**
 * Сервис обработки загрузки медиа контента.
 * Координирует процесс загрузки: проверяет кэш, определяет провайдера,
 * управляет очередью задач и выполняет загрузку файлов.
 */
@Service
public class DownloadService {
    
    /**
     * Обрабатывает URL для загрузки медиа контента.
     * 
     * @param url URL для загрузки
     * @param chatId ID чата Telegram
     * @return Mono с загруженным медиа контентом
     */
    public Mono<MediaContent> processUrl(String url, Long chatId) {
        // Проверяем кэш - если контент уже загружен, возвращаем его
        var cachedContent = cacheRepository.get(url);
        ...
    }
}
```

### Логирование

Все сообщения логов должны быть на русском языке:

```java
log.info("Обработка URL: {} для чата: {}", url, chatId);
log.error("Ошибка загрузки с URL: {}", url, e);
log.debug("Найден кэшированный контент для URL: {}", url);
```

## Архитектура проекта

Проект построен на принципах **гексагональной архитектуры** (Hexagonal Architecture / Ports & Adapters).

### Структура модулей:

```
src/main/java/top/firlian/downloader/
├── domain/              # Ядро приложения (доменная логика)
│   ├── model/           # Доменные модели
│   ├── port/            # Порты (интерфейсы)
│   └── error/           # Доменные исключения
├── application/         # Бизнес-логика (use cases)
├── adapter/             # Адаптеры (инфраструктура)
│   ├── dto/             # Data Transfer Objects
│   ├── in/              # Входные адаптеры (контроллеры, боты)
│   └── out/             # Выходные адаптеры (репозитории, внешние API)
├── config/              # Конфигурация Spring
└── util/                # Утилиты
```

### Принципы архитектуры:

1. **Domain (Доменный слой)** - независимое ядро приложения
   - Содержит бизнес-логику и правила
   - Не зависит от внешних фреймворков
   - Определяет порты (интерфейсы) для взаимодействия

2. **Application (Слой приложения)** - оркестрация бизнес-логики
   - Use cases и сервисы
   - Координирует работу доменных объектов
   - Использует порты для взаимодействия с инфраструктурой

3. **Adapters (Адаптеры)** - реализация портов
   - **Входные адаптеры (in)**: Telegram Bot, REST API
   - **Выходные адаптеры (out)**: yt-dlp, кэш, очередь задач

### Зависимости:

```
Adapters → Application → Domain
```

- Домен не зависит ни от чего
- Application зависит только от Domain
- Адаптеры зависят от Application и Domain

## Правила кодирования

### Общие правила

1. **Используйте Lombok** для уменьшения boilerplate кода:
   - `@RequiredArgsConstructor` для внедрения зависимостей
   - `@Slf4j` для логирования
   - `@Builder` для построителей объектов

2. **Реактивное программирование**:
   - Используйте `Mono` и `Flux` из Project Reactor
   - Избегайте блокирующих операций
   - Используйте `.flatMap()`, `.map()`, `.onErrorResume()` и т.д.

3. **Обработка ошибок**:
   - Используйте доменные исключения из `domain/error/`
   - `ContentUnavailableException` - контент недоступен
   - `DownloadException` - ошибка загрузки
   - `UnsupportedProviderException` - неподдерживаемый источник

4. **Именование**:
   - Классы: PascalCase
   - Методы и переменные: camelCase
   - Константы: UPPER_SNAKE_CASE
   - Пакеты: lowercase

### Стиль кода

```java
// Хорошо
public class MediaDownloaderConfig {
    
    @Value("${downloader.ytdlp.mode:local}")
    private String ytdlpMode;
    
    /**
     * Предоставляет основной MediaDownloader bean на основе конфигурации.
     */
    @Bean
    @Primary
    public MediaDownloader mediaDownloader(
            YtDlpMediaDownloader localDownloader,
            HttpYtDlpMediaDownloader httpDownloader) {
        
        if ("http".equalsIgnoreCase(ytdlpMode)) {
            log.info("Используется режим HTTP сервиса yt-dlp");
            return httpDownloader;
        }
        
        log.info("Используется режим локального бинарного файла yt-dlp");
        return localDownloader;
    }
}
```

### Тестирование

1. Используйте JUnit 5
2. Тесты должны быть в том же пакете, что и тестируемый класс
3. Используйте `@SpringBootTest` для интеграционных тестов
4. Комментарии в тестах также на русском языке

```java
/**
 * Тесты для MediaDownloaderConfig для проверки корректного выбора реализации загрузчика.
 */
@SpringBootTest
class MediaDownloaderConfigTest {
    
    @Test
    void shouldUseLocalDownloader() {
        // Проверяем, что используется локальный загрузчик
        assertNotNull(mediaDownloader);
        assertInstanceOf(YtDlpMediaDownloader.class, mediaDownloader);
    }
}
```

## Конфигурация

### Переменные окружения

Все настройки приложения задаются через переменные окружения:

- `TELEGRAM_BOT_TOKEN` - токен Telegram бота
- `TELEGRAM_BOT_USERNAME` - имя пользователя бота
- `YTDLP_MODE` - режим работы yt-dlp (`local` или `http`)
- `YTDLP_BIN` - путь к бинарному файлу yt-dlp (для local режима)
- `YTDLP_SERVICE_URL` - URL сервиса yt-dlp (для http режима)
- `DOWNLOAD_DIR` - директория для загрузок
- `CACHE_TTL_HOURS` - время жизни кэша в часах
- `SIZE_LIMIT_MB` - максимальный размер файла для прямой отправки

### application.yml

```yaml
downloader:
  ytdlp:
    bin: ${YTDLP_BIN:/usr/local/bin/yt-dlp}
    mode: ${YTDLP_MODE:local}
    service-url: ${YTDLP_SERVICE_URL:http://localhost:8090}
  download-dir: ${DOWNLOAD_DIR:/tmp/downloads}
  cache-ttl-hours: ${CACHE_TTL_HOURS:24}
  size-limit-mb: ${SIZE_LIMIT_MB:50}
```

## Документация

### Markdown файлы

Документация в формате Markdown должна быть:
- `README.md` - основная документация на русском
- `DEVELOPMENT.md` - руководство по разработке на русском
- `QUICKSTART.md` - быстрый старт на русском

### Комментарии в коде

- Однострочные комментарии: `// Комментарий`
- Многострочные комментарии: `/* Комментарий */`
- Javadoc: `/** Документация */`

Все на русском языке!

## Git

### Коммиты

Сообщения коммитов могут быть на английском или русском языке, но предпочтительно на английском для универсальности:

```
Add local development mode with dockerized yt-dlp service
Добавить режим локальной разработки с dockerized yt-dlp сервисом
```

### Ветки

Именование веток на английском:
- `feature/feature-name`
- `bugfix/bug-name`
- `hotfix/issue-name`

## Поддерживаемые платформы

Приложение поддерживает загрузку медиа с:
- YouTube (видео, шорты)
- VK (посты, фото, документы, видео)
- Instagram (посты, Reels, карусели)

При добавлении новых платформ:
1. Добавьте новый Provider в enum
2. Обновите UrlProviderDetector
3. При необходимости создайте специализированный downloader
4. Обновите документацию

## Технологический стек

- **Java 17+**
- **Spring Boot 3.2.0**
- **Gradle 8.5**
- **Project Reactor** - реактивное программирование
- **Telegram Bots API** - интеграция с Telegram
- **yt-dlp** - загрузка медиа
- **Docker** - контейнеризация

## Примеры кода

### Создание нового сервиса

```java
package top.firlian.downloader.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Описание сервиса на русском языке.
 * Детальное описание функциональности.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final SomeDependency dependency;
    
    /**
     * Описание метода на русском языке.
     * 
     * @param param описание параметра
     * @return описание возвращаемого значения
     */
    public Mono<Result> doSomething(String param) {
        log.info("Выполнение операции с параметром: {}", param);
        
        // Реализация логики
        return Mono.just(new Result());
    }
}
```

### Создание нового адаптера

```java
package top.firlian.downloader.adapter.out;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.firlian.downloader.domain.port.SomePort;

/**
 * Реализация адаптера для внешнего сервиса.
 * Описание функциональности адаптера.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalServiceAdapter implements SomePort {
    
    @Override
    public void execute() {
        log.debug("Выполнение операции во внешнем сервисе");
        // Реализация
    }
}
```

## Дополнительные рекомендации

1. **Код должен быть чистым и понятным**
2. **Следуйте SOLID принципам**
3. **Используйте DRY (Don't Repeat Yourself)**
4. **Пишите тесты для новой функциональности**
5. **Обновляйте документацию при изменениях**
6. **Логируйте важные операции и ошибки**
7. **Обрабатывайте ошибки корректно**

## Контакты

Для вопросов по архитектуре и стилю кодирования создавайте issue в репозитории.
