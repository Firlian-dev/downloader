# Настройка для Windows

Это руководство объясняет, как запустить Telegram Downloader Bot на Windows.

## Рекомендуемые подходы

### Вариант 1: HTTP режим с Docker (Рекомендуется)

Это наиболее простой и надежный способ для Windows.

#### Требования
- Docker Desktop для Windows
- Java 17 или выше
- Git

#### Установка

1. **Установите Docker Desktop**
   - Скачайте с https://www.docker.com/products/docker-desktop
   - Запустите Docker Desktop

2. **Клонируйте репозиторий**
   ```powershell
   git clone https://github.com/Firlian-dev/downloader.git
   cd downloader
   ```

3. **Создайте файл конфигурации**
   ```powershell
   copy .env.dev.example .env.dev
   ```

4. **Отредактируйте `.env.dev`** в любом текстовом редакторе:
   ```env
   TELEGRAM_BOT_TOKEN=ваш_токен_бота
   TELEGRAM_BOT_USERNAME=имя_вашего_бота
   YTDLP_MODE=http
   YTDLP_SERVICE_URL=http://localhost:8090
   DOWNLOAD_DIR=./downloads
   ```

5. **Создайте директорию для загрузок**
   ```powershell
   mkdir downloads
   ```

6. **Запустите yt-dlp сервис в Docker**
   ```powershell
   docker-compose -f docker-compose.dev.yml up -d
   ```

7. **Проверьте, что сервис запущен**
   ```powershell
   curl http://localhost:8090/health
   ```

8. **Запустите приложение**
   
   С помощью PowerShell:
   ```powershell
   # Установите переменные окружения
   $env:TELEGRAM_BOT_TOKEN = "ваш_токен"
   $env:TELEGRAM_BOT_USERNAME = "имя_бота"
   $env:YTDLP_MODE = "http"
   $env:YTDLP_SERVICE_URL = "http://localhost:8090"
   $env:DOWNLOAD_DIR = "./downloads"
   
   # Запустите приложение
   .\gradlew.bat bootRun
   ```

### Вариант 2: Локальный режим с yt-dlp

Если вы хотите запустить приложение без Docker (не рекомендуется, так как требует ручной установки yt-dlp).

#### Требования
- Java 17 или выше
- Python 3.8+ и pip
- ffmpeg

#### Установка

1. **Установите yt-dlp**
   ```powershell
   pip install yt-dlp
   ```

2. **Установите ffmpeg**
   - Скачайте с https://ffmpeg.org/download.html
   - Добавьте в PATH

3. **Проверьте установку**
   ```powershell
   yt-dlp --version
   ffmpeg -version
   ```

4. **Создайте файл конфигурации**
   ```powershell
   copy .env.example .env
   ```

5. **Отредактируйте `.env`**:
   ```env
   TELEGRAM_BOT_TOKEN=ваш_токен_бота
   TELEGRAM_BOT_USERNAME=имя_вашего_бота
   YTDLP_MODE=local
   YTDLP_BIN=yt-dlp
   DOWNLOAD_DIR=./downloads
   ```

6. **Создайте директорию для загрузок**
   ```powershell
   mkdir downloads
   ```

7. **Запустите приложение**
   ```powershell
   $env:TELEGRAM_BOT_TOKEN = "ваш_токен"
   $env:TELEGRAM_BOT_USERNAME = "имя_бота"
   $env:YTDLP_MODE = "local"
   $env:YTDLP_BIN = "yt-dlp"
   $env:DOWNLOAD_DIR = "./downloads"
   
   .\gradlew.bat bootRun
   ```

### Вариант 3: Полный Docker (Продакшн)

Запустите все в Docker.

#### Установка

1. **Установите Docker Desktop**

2. **Клонируйте репозиторий**
   ```powershell
   git clone https://github.com/Firlian-dev/downloader.git
   cd downloader
   ```

3. **Создайте файл `.env`**
   ```powershell
   copy .env.example .env
   ```

4. **Отредактируйте `.env`**:
   ```env
   TELEGRAM_BOT_TOKEN=ваш_токен_бота
   TELEGRAM_BOT_USERNAME=имя_вашего_бота
   ```

5. **Запустите все сервисы**
   ```powershell
   docker-compose up -d
   ```

6. **Проверьте логи**
   ```powershell
   docker-compose logs -f
   ```

## Возможные проблемы

### Ошибка "Cannot run program yt-dlp"

**Проблема**: Приложение не может найти yt-dlp.

**Решение**:
- Используйте HTTP режим вместо локального
- Или установите yt-dlp и укажите полный путь в `YTDLP_BIN`

### Ошибка "500 Internal Server Error from yt-dlp service"

**Проблема**: yt-dlp сервис не может загрузить контент (часто с Instagram).

**Возможные причины**:
- Instagram блокирует запросы без аутентификации
- URL содержит приватный контент
- yt-dlp нужно обновить

**Решение**:
```powershell
# Остановите сервис
docker-compose -f docker-compose.dev.yml down

# Пересоберите с обновленной версией yt-dlp
docker-compose -f docker-compose.dev.yml build --no-cache

# Запустите снова
docker-compose -f docker-compose.dev.yml up -d
```

### Пути с обратным слешем

**Проблема**: Windows использует обратные слеши `\`, но приложение ожидает прямые `/`.

**Решение**: Используйте относительные пути:
- ✅ Правильно: `./downloads` или `downloads`
- ❌ Неправильно: `C:\Users\username\downloads`

## Структура директорий

```
downloader/
├── downloads/          # Загруженные файлы (создается автоматически)
├── src/                # Исходный код
├── .env                # Конфигурация для продакшн
├── .env.dev            # Конфигурация для разработки
└── docker-compose.yml  # Docker конфигурация
```

## Проверка работы

После запуска:

1. Найдите вашего бота в Telegram
2. Отправьте `/start` - должно прийти приветственное сообщение
3. Отправьте ссылку YouTube, например: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
4. Бот должен начать загрузку и отправить файл

## Поддержка

Если возникли проблемы:
1. Проверьте логи приложения
2. Проверьте логи Docker: `docker-compose logs`
3. Создайте Issue в GitHub с описанием проблемы и логами
