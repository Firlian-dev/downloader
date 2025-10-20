# Руководство по разработке

## Архитектура

Проект следует принципам **гексагональной архитектуры** (Hexagonal Architecture / Ports & Adapters).

### Слои

1. **Domain Layer** (`domain/`)
   - Независим от инфраструктуры
   - Содержит бизнес-логику
   - Определяет порты (интерфейсы)

2. **Application Layer** (`application/`)
   - Координирует работу домена
   - Реализует use cases
   - Не зависит от конкретных адаптеров

3. **Adapter Layer** (`adapter/`)
   - Реализует порты
   - Интегрируется с внешними системами
   - Telegram Bot, yt-dlp и т.д.

4. **Utilities** (`util/`)
   - Вспомогательные классы
   - In-memory реализации для простоты

### Зависимости между слоями

```
Adapter → Application → Domain
  ↓
Util → Domain (только интерфейсы)
```

## Добавление нового провайдера

1. Добавьте новое значение в `Provider` enum:
```java
public enum Provider {
    YOUTUBE,
    VK,
    INSTAGRAM,
    TIKTOK  // новый провайдер
}
```

2. Обновите `UrlProviderDetector`:
```java
private static final Pattern TIKTOK_PATTERN = Pattern.compile(
    "(https?://)?(www\\.)?(tiktok\\.com).*"
);

// В методе detectProvider:
if (TIKTOK_PATTERN.matcher(normalizedUrl).matches()) {
    return Provider.TIKTOK;
}
```

3. Добавьте тест:
```java
@Test
void testDetectTikTok() {
    assertEquals(Provider.TIKTOK, 
        detector.detectProvider("https://www.tiktok.com/@user/video/123"));
}
```

## Запуск в режиме разработки

### Автоматическая перезагрузка при изменениях

```bash
./gradlew bootRun --continuous
```

### Запуск с отладкой

```bash
./gradlew bootRun --debug-jvm
```

Затем подключите отладчик к порту 5005.

## Тестирование

### Запуск всех тестов

```bash
./gradlew test
```

### Запуск конкретного теста

```bash
./gradlew test --tests UrlProviderDetectorTest
```

### Генерация отчета о покрытии

```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Проверка кода

### Форматирование

Проект использует стандартное форматирование Java. 

### Статический анализ

Запустите проверку:
```bash
./gradlew check
```

## Сборка Docker образа

### Локальная сборка

```bash
docker build -t telegram-downloader .
```

### Запуск собранного образа

```bash
docker run -e TELEGRAM_BOT_TOKEN=your_token telegram-downloader
```

## Отладка

### Логирование

Уровни логирования настраиваются в `application.yml`:

```yaml
logging:
  level:
    top.firlian.downloader: DEBUG
    org.telegram.telegrambots: INFO
```

### Проверка работы yt-dlp

```bash
# Проверка установки
yt-dlp --version

# Тест скачивания (без фактического скачивания)
yt-dlp --skip-download --dump-json "URL"
```

## Частые проблемы

### Telegram API Rate Limits

При частых запросах Telegram может ограничить скорость отправки:
- Добавьте задержки между отправками
- Используйте BatchSender для группировки сообщений

### Большие файлы

Telegram имеет ограничение на размер файлов (50 МБ для ботов):
- Настройте `SIZE_LIMIT_MB` в конфигурации
- Для больших файлов отправляйте URL вместо файла

### Проблемы с yt-dlp

- Обновляйте yt-dlp регулярно: `pip install -U yt-dlp`
- Некоторые сайты могут блокировать скачивание
- Используйте cookies для приватного контента

## Git Workflow

1. Создайте ветку для новой функции:
```bash
git checkout -b feature/new-feature
```

2. Делайте небольшие коммиты:
```bash
git commit -m "Add TikTok provider detection"
```

3. Запустите тесты перед коммитом:
```bash
./gradlew test
```

4. Создайте Pull Request

## Полезные команды

```bash
# Очистка build директории
./gradlew clean

# Показать зависимости
./gradlew dependencies

# Обновить Gradle wrapper
./gradlew wrapper --gradle-version=8.5

# Показать доступные задачи
./gradlew tasks
```
