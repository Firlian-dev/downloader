# Quick Start Guide

## Быстрый старт с Docker

1. Получите токен бота у [@BotFather](https://t.me/BotFather) в Telegram

2. Создайте файл `.env`:
```bash
cat > .env << EOF
TELEGRAM_BOT_TOKEN=YOUR_BOT_TOKEN_HERE
TELEGRAM_BOT_USERNAME=your_bot_username
EOF
```

3. Запустите бот:
```bash
docker-compose up -d
```

4. Проверьте логи:
```bash
docker-compose logs -f
```

5. Найдите бота в Telegram и отправьте `/start`

## Локальный запуск для разработки

### Вариант 1: С dockerized yt-dlp (рекомендуется)

1. Запустите yt-dlp сервис:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

2. Настройте переменные окружения:
```bash
cp .env.dev.example .env.dev
# Отредактируйте .env.dev и укажите токен бота
```

3. Запустите приложение:
```bash
export $(cat .env.dev | xargs) && ./gradlew bootRun
```

Подробнее см. [DEVELOPMENT.md](DEVELOPMENT.md)

### Вариант 2: С локальным yt-dlp

1. Установите зависимости:
```bash
# Java 17+
java -version

# yt-dlp
pip3 install yt-dlp
# или
brew install yt-dlp
```

2. Настройте переменные окружения:
```bash
export TELEGRAM_BOT_TOKEN=your_token_here
export TELEGRAM_BOT_USERNAME=your_bot_username
export YTDLP_MODE=local
export YTDLP_BIN=/usr/local/bin/yt-dlp
export DOWNLOAD_DIR=/tmp/downloads
```

3. Запустите приложение:
```bash
./gradlew bootRun
```

## Тестирование

Запустите тесты:
```bash
./gradlew test
```

Соберите проект:
```bash
./gradlew build
```

## Примеры использования

После запуска бота, отправьте ему любую из следующих ссылок:

### YouTube
```
https://www.youtube.com/watch?v=dQw4w9WgXcQ
https://youtu.be/dQw4w9WgXcQ
https://www.youtube.com/shorts/abc123
```

### VK
```
https://vk.com/wall-12345_67890
https://vk.com/video12345_67890
```

### Instagram
```
https://www.instagram.com/p/ABC123/
https://www.instagram.com/reel/XYZ789/
```

## Структура проекта

```
downloader/
├── src/
│   ├── main/
│   │   ├── java/top/firlian/downloader/
│   │   │   ├── domain/          # Доменная логика
│   │   │   │   ├── model/       # Модели данных
│   │   │   │   ├── port/        # Интерфейсы портов
│   │   │   │   └── error/       # Исключения
│   │   │   ├── application/     # Бизнес-логика
│   │   │   ├── adapter/         # Адаптеры
│   │   │   │   └── out/         # Внешние адаптеры
│   │   │   └── util/            # Утилиты
│   │   └── resources/
│   │       └── application.yml  # Конфигурация
│   └── test/                    # Тесты
├── Dockerfile                   # Образ Docker
├── docker-compose.yml          # Конфигурация Docker Compose
└── build.gradle                # Конфигурация Gradle
```

## Устранение неполадок

### Бот не отвечает
- Проверьте токен в переменных окружения
- Убедитесь, что бот запущен: `docker-compose ps`
- Проверьте логи: `docker-compose logs -f`

### Ошибка скачивания
- Убедитесь, что yt-dlp установлен
- Проверьте доступность ссылки в браузере
- Некоторый контент может быть защищен или недоступен

### Файл не загружается
- Проверьте размер файла (лимит по умолчанию: 50 МБ)
- Большие файлы отправляются как путь к файлу

## Поддержка

Создайте issue в репозитории для сообщения об ошибках или запроса новых функций.
