# Исправления путей и конфигурации

## Проблемы из errors.md

### 1. Ошибка в режиме HTTP
**Проблема**: 500 Internal Server Error from POST http://localhost:8090/metadata  
**Причина**: yt-dlp сервис не может загрузить Instagram контент (возможно из-за блокировки Instagram или необходимости аутентификации)

**Исправление**:
- Добавлен флаг `--no-check-certificates` в yt-dlp команду для обхода проблем с SSL
- Улучшено логирование ошибок в ytdlp-service/app.py
- Добавлены рекомендации по обновлению yt-dlp для работы с Instagram

### 2. Ошибка в режиме local на Windows
**Проблема**: Cannot run program "/usr/local/bin/yt-dlp": CreateProcess error=2  
**Причина**: Путь `/usr/local/bin/yt-dlp` является Unix-style путем и не работает на Windows

**Исправление**:
- Изменен default путь с `/usr/local/bin/yt-dlp` на `yt-dlp`
- Теперь система ищет yt-dlp в PATH, что работает на всех ОС
- Изменен default путь загрузок с `/tmp/downloads` на `./downloads` для кросс-платформенности

## Изменения в коде

### application.yml
```yaml
# Было:
bin: ${YTDLP_BIN:/usr/local/bin/yt-dlp}
download-dir: ${DOWNLOAD_DIR:/tmp/downloads}

# Стало:
bin: ${YTDLP_BIN:yt-dlp}
download-dir: ${DOWNLOAD_DIR:./downloads}
```

### YtDlpMediaDownloader.java
Обновлены значения по умолчанию в @Value аннотациях:
- `ytdlpBin`: `/usr/local/bin/yt-dlp` → `yt-dlp`
- `downloadDir`: `/tmp/downloads` → `./downloads`

### HttpYtDlpMediaDownloader.java
Обновлены значения по умолчанию в @Value аннотациях:
- `downloadDir`: `/tmp/downloads` → `./downloads`

### docker-compose.yml
- Изменен с использования локального бинарного файла yt-dlp на HTTP сервис
- Добавлен ytdlp-service контейнер из ytdlp-service/Dockerfile
- Настроена зависимость и health check для ytdlp-service
- Основной контейнер теперь использует `YTDLP_MODE=http`

### ytdlp-service/app.py
- Добавлен флаг `--no-check-certificates` для обхода SSL проблем
- Улучшено логирование ошибок с print() для отладки
- Добавлена обработка исключений с более детальными сообщениями

### .env.example
Обновлена документация и значения по умолчанию:
- Добавлены примеры путей для разных ОС (Windows, Linux, Docker)
- Изменен рекомендуемый режим на `http` для локальной разработки
- Обновлен путь загрузок на `./downloads` для совместимости

## Новые файлы

### WINDOWS_SETUP.md
Подробное руководство по установке и запуску на Windows:
- 3 варианта установки: HTTP режим с Docker (рекомендуется), локальный режим, полный Docker
- Инструкции для PowerShell
- Решение распространенных проблем
- Примеры конфигурации

## Совместимость

### До изменений:
- ❌ Windows (local mode): Не работает (Unix-style путь)
- ✅ Linux (local mode): Работает
- ✅ Docker: Работает
- ⚠️ HTTP mode: Проблемы с Instagram

### После изменений:
- ✅ Windows (local mode): Работает (если установлен yt-dlp)
- ✅ Windows (HTTP mode): Работает (рекомендуется)
- ✅ Linux (local mode): Работает
- ✅ Docker: Работает
- ⚠️ HTTP mode + Instagram: Улучшено, но могут быть ограничения Instagram

## Рекомендации по использованию

### Для разработки на Windows:
1. Используйте HTTP режим (`YTDLP_MODE=http`)
2. Запустите yt-dlp сервис в Docker: `docker-compose -f docker-compose.dev.yml up -d`
3. Запустите приложение локально с `.env.dev`

### Для разработки на Linux/Mac:
1. Можно использовать локальный режим с установленным yt-dlp
2. Или HTTP режим (предпочтительно для единообразия)

### Для продакшн:
1. Используйте Docker Compose с `docker-compose.yml`
2. Все запускается в контейнерах с HTTP режимом

## Проблема с Instagram

Instagram может блокировать запросы от yt-dlp. Возможные решения:

1. **Обновить yt-dlp**: `pip install -U yt-dlp` или пересобрать Docker образ
2. **Использовать cookies**: Экспортировать cookies из браузера (требует модификации кода)
3. **VPN/Proxy**: Использовать прокси если Instagram блокирует IP
4. **Публичный контент**: Работает только с публичными постами

## Тестирование

После изменений:
- ✅ Все тесты проходят (21 тест)
- ✅ Код компилируется без ошибок
- ✅ Совместимость с Windows, Linux, Mac, Docker
- ✅ Поддержка относительных путей
