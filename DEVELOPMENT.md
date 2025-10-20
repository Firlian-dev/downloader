# Local Development Guide

This guide explains how to run the application locally for development and debugging, while using a dockerized yt-dlp service.

## Overview

For local development, we use a hybrid approach:
- **Main application** runs locally (not in Docker) - easier to debug and faster iterations
- **yt-dlp service** runs in Docker - consistent environment, no need to install yt-dlp locally

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Telegram Bot token (get from [@BotFather](https://t.me/BotFather))

## Setup

### 1. Start the yt-dlp service in Docker

```bash
# Start only the yt-dlp service
docker-compose -f docker-compose.dev.yml up -d

# Check it's running
docker-compose -f docker-compose.dev.yml ps

# Check logs
docker-compose -f docker-compose.dev.yml logs -f
```

The yt-dlp service will be available at `http://localhost:8090` and provides the following endpoints:
- `GET /health` - Health check
- `GET /version` - Get yt-dlp version
- `POST /metadata` - Get metadata for a URL
- `POST /download` - Download media from URL

### 2. Configure environment variables

Create a `.env.dev` file from the example:

```bash
cp .env.dev.example .env.dev
```

Edit `.env.dev` and set your bot token:

```env
TELEGRAM_BOT_TOKEN=your_actual_bot_token_here
TELEGRAM_BOT_USERNAME=your_bot_username
YTDLP_MODE=http
YTDLP_SERVICE_URL=http://localhost:8090
DOWNLOAD_DIR=./downloads
```

**Important settings for local development:**
- `YTDLP_MODE=http` - Use HTTP service instead of local binary
- `YTDLP_SERVICE_URL=http://localhost:8090` - URL of dockerized yt-dlp service
- `DOWNLOAD_DIR=./downloads` - Shared directory with Docker container

### 3. Create the downloads directory

```bash
mkdir -p downloads
```

This directory is shared between your local app and the Docker container.

### 4. Run the application locally

You can run the application in several ways:

#### Option A: Using Gradle (recommended for development)

```bash
# Load environment variables and run
export $(cat .env.dev | xargs) && ./gradlew bootRun
```

#### Option B: Using your IDE

1. Open the project in IntelliJ IDEA or your preferred IDE
2. Configure environment variables in Run/Debug configuration:
   - Copy values from `.env.dev`
   - Or use EnvFile plugin to load `.env.dev` automatically
3. Run `DownloaderApplication` main class

#### Option C: Build and run JAR

```bash
# Build the application
./gradlew build

# Load environment variables and run
export $(cat .env.dev | xargs) && java -jar build/libs/downloader-0.0.1-SNAPSHOT.jar
```

## Development Workflow

### Making code changes

1. Make your changes to the Java code
2. Stop the application (Ctrl+C)
3. Restart with `./gradlew bootRun` or from your IDE
4. The yt-dlp service keeps running - no need to restart it

### Testing the yt-dlp service

You can test the yt-dlp service independently:

```bash
# Check health
curl http://localhost:8090/health

# Get version
curl http://localhost:8090/version

# Get metadata
curl -X POST http://localhost:8090/metadata \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'

# Download a video
curl -X POST http://localhost:8090/download \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'
```

### Debugging

#### Debug the main application
- Set breakpoints in your IDE
- Run in debug mode
- All Java debugging features work normally

#### Debug yt-dlp issues
- Check Docker logs: `docker-compose -f docker-compose.dev.yml logs -f ytdlp-service`
- Check downloaded files: `ls -la downloads/`
- Test yt-dlp service directly with curl (see above)

### Viewing logs

```bash
# Application logs - visible in your terminal/IDE
# No special setup needed

# yt-dlp service logs
docker-compose -f docker-compose.dev.yml logs -f ytdlp-service
```

## Switching Between Modes

### Local mode (using local yt-dlp binary)

If you want to use a local yt-dlp installation instead of Docker:

1. Install yt-dlp locally:
   ```bash
   # macOS
   brew install yt-dlp
   
   # Linux
   pip3 install yt-dlp
   ```

2. Update environment:
   ```bash
   export YTDLP_MODE=local
   export YTDLP_BIN=/usr/local/bin/yt-dlp
   ```

3. Stop the Docker service if running:
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

### HTTP mode (using Docker - recommended for development)

```bash
export YTDLP_MODE=http
export YTDLP_SERVICE_URL=http://localhost:8090
```

## Troubleshooting

### Application can't connect to yt-dlp service

1. Check if the service is running:
   ```bash
   docker-compose -f docker-compose.dev.yml ps
   ```

2. Check if the service is healthy:
   ```bash
   curl http://localhost:8090/health
   ```

3. Check logs:
   ```bash
   docker-compose -f docker-compose.dev.yml logs ytdlp-service
   ```

4. Restart the service:
   ```bash
   docker-compose -f docker-compose.dev.yml restart
   ```

### Downloads fail

1. Check if downloads directory exists and is writable:
   ```bash
   ls -la downloads/
   ```

2. Check yt-dlp service logs:
   ```bash
   docker-compose -f docker-compose.dev.yml logs ytdlp-service
   ```

3. Test download manually:
   ```bash
   curl -X POST http://localhost:8090/download \
     -H "Content-Type: application/json" \
     -d '{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'
   ```

### Port 8090 already in use

Change the port mapping in `docker-compose.dev.yml`:

```yaml
ports:
  - "9090:8090"  # Map to different host port
```

Then update `YTDLP_SERVICE_URL`:
```bash
export YTDLP_SERVICE_URL=http://localhost:9090
```

### Environment variables not loaded

Make sure to export variables before running:

```bash
export $(cat .env.dev | xargs)
./gradlew bootRun
```

Or use an IDE plugin to load `.env.dev` automatically.

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests UrlProviderDetectorTest

# Run with verbose output
./gradlew test --info
```

## Cleanup

### Stop the yt-dlp service

```bash
docker-compose -f docker-compose.dev.yml down
```

### Remove downloaded files

```bash
rm -rf downloads/*
```

### Rebuild yt-dlp service (after changes)

```bash
docker-compose -f docker-compose.dev.yml build
docker-compose -f docker-compose.dev.yml up -d
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Local Machine (Host)                               │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Spring Boot Application (Port varies)      │   │
│  │  - Running in IDE/Terminal                  │   │
│  │  - Easy to debug                            │   │
│  │  - Fast restart                             │   │
│  └──────────────┬──────────────────────────────┘   │
│                 │                                   │
│                 │ HTTP (localhost:8090)             │
│                 │                                   │
│  ┌──────────────▼──────────────────────────────┐   │
│  │  Docker Container                           │   │
│  │  ┌────────────────────────────────────┐    │   │
│  │  │  yt-dlp HTTP Service (Port 8090)   │    │   │
│  │  │  - Python Flask API                │    │   │
│  │  │  - yt-dlp + ffmpeg                 │    │   │
│  │  └────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  Shared Volume: ./downloads                         │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Production Deployment

For production, use the standard `docker-compose.yml` which runs everything in Docker:

```bash
# Stop development setup
docker-compose -f docker-compose.dev.yml down

# Start production setup
docker-compose up -d
```

## Tips

1. **Keep yt-dlp service running** - It starts quickly and doesn't consume much resources
2. **Use IDE run configurations** - Save environment variables in your IDE
3. **Hot reload** - Use Spring DevTools for faster development (add to dependencies if needed)
4. **Test in isolation** - Test the yt-dlp service independently before running the full app
5. **Check logs** - Both application and Docker logs when debugging issues

## Further Reading

- [Spring Boot Documentation](https://spring.boot.io/docs)
- [yt-dlp Documentation](https://github.com/yt-dlp/yt-dlp)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
