# Local Development Setup - Implementation Summary

## Overview

This implementation adds support for running the Telegram Downloader Bot application locally for development and debugging, while using a containerized yt-dlp service in Docker. This provides the best of both worlds:

- **Local Application**: Easy debugging, fast iterations, IDE integration
- **Dockerized yt-dlp**: Consistent environment, no local dependencies to install

## Problem Solved

Previously, developers had two options:
1. Run everything in Docker (harder to debug)
2. Install yt-dlp locally (inconsistent environments)

Now there's a third, recommended option:
3. **Run app locally + yt-dlp in Docker** (best for development)

## Changes Made

### 1. yt-dlp HTTP Service (`ytdlp-service/`)

Created a new lightweight Flask-based HTTP wrapper for yt-dlp:

**Files:**
- `ytdlp-service/app.py` - Flask API with endpoints for metadata and download
- `ytdlp-service/requirements.txt` - Python dependencies (Flask)
- `ytdlp-service/Dockerfile` - Docker image with yt-dlp, ffmpeg, and Flask
- `ytdlp-service/README.md` - Service documentation

**Endpoints:**
- `GET /health` - Health check
- `GET /version` - Get yt-dlp version
- `POST /metadata` - Get video metadata
- `POST /download` - Download media

### 2. HTTP Client Adapter

**File:** `src/main/java/top/firlian/downloader/adapter/out/HttpYtDlpMediaDownloader.java`

New MediaDownloader implementation that:
- Communicates with yt-dlp service via HTTP
- Uses Spring WebClient for reactive HTTP calls
- Implements same interface as local downloader
- Handles metadata extraction and downloads remotely

### 3. Configuration Support

**File:** `src/main/java/top/firlian/downloader/config/MediaDownloaderConfig.java`

Configuration class that:
- Selects appropriate MediaDownloader based on `YTDLP_MODE`
- Supports `local` mode (existing behavior) or `http` mode (new)
- Uses Spring's `@Primary` annotation for bean selection

**Configuration changes:**
- `src/main/resources/application.yml` - Added mode and service-url settings
- `.env.example` - Updated with new configuration options
- `.env.dev.example` - New file for development setup

### 4. Development Docker Compose

**File:** `docker-compose.dev.yml`

New Docker Compose file for local development:
- Runs only the yt-dlp service
- Maps port 8090 to host
- Shares `./downloads` directory
- Includes health check

### 5. Documentation

**New files:**
- `DEVELOPMENT.md` - Comprehensive local development guide
- `ytdlp-service/README.md` - yt-dlp service documentation
- `test-ytdlp-service.sh` - Service verification script

**Updated files:**
- `README.md` - Added local development section
- `QUICKSTART.md` - Updated with new setup options
- `.gitignore` - Added .env.dev

### 6. Tests

**File:** `src/test/java/top/firlian/downloader/config/MediaDownloaderConfigTest.java`

Unit tests for configuration:
- Verifies context loads correctly
- Tests local mode selection
- Tests HTTP mode selection

## How It Works

### Architecture

```
┌────────────────────────────────────────┐
│  Host Machine                          │
│                                        │
│  ┌──────────────────────────────┐     │
│  │  Spring Boot App             │     │
│  │  (Running in IDE/Terminal)   │     │
│  │                              │     │
│  │  YTDLP_MODE=http            │     │
│  │  YTDLP_SERVICE_URL=         │     │
│  │    http://localhost:8090    │     │
│  └──────────┬───────────────────┘     │
│             │                          │
│             │ HTTP Requests            │
│             ▼                          │
│  ┌──────────────────────────────┐     │
│  │  Docker Container            │     │
│  │  ┌────────────────────────┐  │     │
│  │  │ yt-dlp HTTP Service    │  │     │
│  │  │ (Port 8090)            │  │     │
│  │  │                        │  │     │
│  │  │ - Flask API            │  │     │
│  │  │ - yt-dlp CLI           │  │     │
│  │  │ - ffmpeg               │  │     │
│  │  └────────────────────────┘  │     │
│  └──────────────────────────────┘     │
│                                        │
│  Shared: ./downloads/                  │
└────────────────────────────────────────┘
```

### Configuration Modes

1. **Local Mode** (default)
   ```bash
   YTDLP_MODE=local
   YTDLP_BIN=/usr/local/bin/yt-dlp
   ```
   - Uses `YtDlpMediaDownloader`
   - Executes yt-dlp binary directly
   - Requires yt-dlp installed locally

2. **HTTP Mode** (new, for development)
   ```bash
   YTDLP_MODE=http
   YTDLP_SERVICE_URL=http://localhost:8090
   ```
   - Uses `HttpYtDlpMediaDownloader`
   - Calls yt-dlp service via HTTP
   - No local yt-dlp installation needed

## Usage

### Quick Start

1. Start yt-dlp service:
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. Configure environment:
   ```bash
   cp .env.dev.example .env.dev
   # Edit .env.dev with your bot token
   ```

3. Run application:
   ```bash
   export $(cat .env.dev | xargs) && ./gradlew bootRun
   ```

### Testing the Service

```bash
# Run test script
./test-ytdlp-service.sh

# Or test manually
curl http://localhost:8090/health
```

### Switching Modes

Change `YTDLP_MODE` in your environment:
- `local` - Use local yt-dlp binary
- `http` - Use Docker service

## Benefits

### For Developers
1. **Easier Debugging**: Run app in IDE with full debugging support
2. **Faster Iterations**: No need to rebuild Docker images
3. **Better IDE Integration**: Auto-completion, refactoring, etc.
4. **No Local Dependencies**: Don't need to install yt-dlp/ffmpeg

### For the Project
1. **Consistent Environment**: yt-dlp version consistent across developers
2. **Easy Testing**: Can test yt-dlp service independently
3. **Flexible Deployment**: Same code works in both modes
4. **Backward Compatible**: Existing local mode still works

## Testing

All tests pass:
```bash
./gradlew test
```

New tests added:
- `MediaDownloaderConfigTest` - Configuration selection logic

## Production Deployment

This change doesn't affect production deployment:
- Use existing `docker-compose.yml` for production
- Default mode is `local`, maintaining existing behavior
- HTTP service only needed for local development

## Files Changed

**New Files:**
- `ytdlp-service/` directory (4 files)
- `docker-compose.dev.yml`
- `.env.dev.example`
- `DEVELOPMENT.md`
- `test-ytdlp-service.sh`
- `src/main/java/top/firlian/downloader/adapter/out/HttpYtDlpMediaDownloader.java`
- `src/main/java/top/firlian/downloader/config/MediaDownloaderConfig.java`
- `src/test/java/top/firlian/downloader/config/MediaDownloaderConfigTest.java`

**Modified Files:**
- `src/main/resources/application.yml`
- `.env.example`
- `.gitignore`
- `README.md`
- `QUICKSTART.md`

**Total:** 16 files (9 new, 7 modified)

## Next Steps

To use this setup:

1. **Start the service:**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Verify it's working:**
   ```bash
   ./test-ytdlp-service.sh
   ```

3. **Configure your bot:**
   ```bash
   cp .env.dev.example .env.dev
   nano .env.dev  # Add your bot token
   ```

4. **Run locally:**
   ```bash
   export $(cat .env.dev | xargs) && ./gradlew bootRun
   ```

5. **Start developing!**
   - Set breakpoints in your IDE
   - Make code changes
   - Restart the app quickly
   - yt-dlp service keeps running

## Support

For issues or questions:
- See `DEVELOPMENT.md` for detailed setup guide
- See `ytdlp-service/README.md` for service API details
- Run `./test-ytdlp-service.sh` to diagnose problems

## Summary

This implementation successfully enables local development with dockerized yt-dlp, making it easier to develop and debug the Telegram Downloader Bot while maintaining a consistent and isolated yt-dlp environment. The changes are minimal, backward-compatible, and well-documented.
