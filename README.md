# Telegram Downloader Bot

Telegram-бот для скачивания медиа-контента из Instagram, VK и YouTube, реализованный на Java с использованием Spring Boot и hexagonal architecture.

## Возможности

- **Поддерживаемые платформы:**
  - YouTube (видео, шорты)
  - VK (посты, фото, документы, видео)
  - Instagram (посты, Reels, карусели)

- **Функции:**
  - Автоматическое определение источника по URL
  - Кэширование скачанных файлов
  - Очередь задач для предотвращения дублирования
  - Поддержка множественных медиа (карусели, плейлисты)
  - Инлайн-клавиатура для выбора элементов
  - Автоматическая отправка файла или URL в зависимости от размера

## Технологический стек

- Java 17+
- Spring Boot 3
- Gradle
- TelegramBots Spring Boot Starter
- Reactor WebClient
- yt-dlp (внешняя утилита)
- Docker

## Архитектура

Проект построен на принципах **гексагональной архитектуры** (Hexagonal Architecture):

```
src/main/java/top/firlian/downloader/
├── domain/          # Ядро приложения
│   ├── model/       # Доменные модели
│   ├── port/        # Порты (интерфейсы)
│   └── error/       # Доменные исключения
├── application/     # Бизнес-логика
├── adapter/         # Адаптеры
│   ├── dto/         # Data Transfer Objects
│   └── out/         # Выходные адаптеры
└── util/            # Утилиты
```

## Требования

- Java 17 или выше
- Docker и Docker Compose (для запуска в контейнере)
- yt-dlp (для локального запуска)

## Установка и запуск

### С использованием Docker (рекомендуется)

1. Клонируйте репозиторий:
```bash
git clone https://github.com/Firlian-dev/downloader.git
cd downloader
```

2. Создайте файл `.env` на основе `.env.example`:
```bash
cp .env.example .env
```

3. Отредактируйте `.env` и укажите ваш токен Telegram бота:
```env
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_BOT_USERNAME=your_bot_username
```

4. Запустите приложение:
```bash
docker-compose up -d
```

### Локальный запуск для разработки (рекомендуется)

Для разработки и отладки рекомендуется запускать приложение локально, используя dockerized yt-dlp сервис:

1. Запустите yt-dlp сервис в Docker:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

2. Создайте `.env.dev` файл:
```bash
cp .env.dev.example .env.dev
# Отредактируйте и укажите ваш токен
```

3. Запустите приложение локально:
```bash
export $(cat .env.dev | xargs) && ./gradlew bootRun
```

Подробнее см. [DEVELOPMENT.md](DEVELOPMENT.md)

### Локальный запуск с локальным yt-dlp

Если вы предпочитаете использовать локально установленный yt-dlp:

1. Установите yt-dlp:
```bash
# macOS
brew install yt-dlp

# Linux
pip3 install yt-dlp

# Или скачайте бинарник с https://github.com/yt-dlp/yt-dlp
```

2. Настройте переменные окружения:
```bash
export TELEGRAM_BOT_TOKEN=your_bot_token_here
export TELEGRAM_BOT_USERNAME=your_bot_username
export YTDLP_MODE=local
export YTDLP_BIN=/usr/local/bin/yt-dlp
export DOWNLOAD_DIR=/tmp/downloads
```

3. Соберите и запустите приложение:
```bash
./gradlew build
./gradlew bootRun
```

## Конфигурация

Приложение настраивается через переменные окружения:

| Переменная | Описание | Значение по умолчанию |
|-----------|----------|----------------------|
| `TELEGRAM_BOT_TOKEN` | Токен Telegram бота (обязательно) | - |
| `TELEGRAM_BOT_USERNAME` | Имя пользователя бота | `downloader_bot` |
| `YTDLP_MODE` | Режим работы yt-dlp: `local` или `http` | `local` |
| `YTDLP_BIN` | Путь к исполняемому файлу yt-dlp (для `local` режима) | `/usr/local/bin/yt-dlp` |
| `YTDLP_SERVICE_URL` | URL сервиса yt-dlp (для `http` режима) | `http://localhost:8090` |
| `DOWNLOAD_DIR` | Директория для скачанных файлов | `/tmp/downloads` |
| `CACHE_TTL_HOURS` | Время жизни кэша в часах | `24` |
| `SIZE_LIMIT_MB` | Максимальный размер файла для прямой отправки (МБ) | `50` |

## Использование

1. Найдите вашего бота в Telegram и начните диалог командой `/start`
2. Отправьте боту ссылку на медиа из поддерживаемых источников
3. Дождитесь скачивания и получите файл или ссылку для скачивания

### Примеры ссылок

- YouTube: `https://www.youtube.com/watch?v=VIDEO_ID`
- YouTube Shorts: `https://www.youtube.com/shorts/SHORT_ID`
- VK: `https://vk.com/wall-123456_789`
- Instagram: `https://www.instagram.com/p/POST_ID/`
- Instagram Reels: `https://www.instagram.com/reel/REEL_ID/`

## Обработка ошибок

- **Приватный контент:** "Контент недоступен"
- **Ошибка скачивания:** "Ошибка загрузки. Попробуйте позже"
- **Неподдерживаемый источник:** "Источник не поддерживается"

## Тестирование

Запуск тестов:
```bash
./gradlew test
```

## Разработка

### Структура проекта

- `domain/` - Доменная логика, независимая от инфраструктуры
- `application/` - Use cases и сервисы приложения
- `adapter/` - Адаптеры для внешних систем (Telegram, yt-dlp)
- `util/` - Вспомогательные утилиты

### Добавление нового источника

1. Добавьте новый провайдер в `Provider` enum
2. Обновите `UrlProviderDetector` для определения нового источника
3. При необходимости создайте специализированный downloader

## Лицензия

См. техническое задание в файле `Техническое задание.md`

## Контакты

Для вопросов и предложений создавайте issue в репозитории.
