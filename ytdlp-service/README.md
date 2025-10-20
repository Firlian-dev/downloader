# yt-dlp HTTP Service

This directory contains a lightweight HTTP wrapper service for yt-dlp. It provides a REST API for executing yt-dlp commands remotely, which is useful for local development and debugging.

## Purpose

When developing the main application locally, you can use this service running in Docker instead of installing yt-dlp locally. This provides:
- Consistent environment across different development machines
- No need to install yt-dlp and ffmpeg locally
- Easy to update yt-dlp version
- Isolated from local system

## API Endpoints

### GET /health
Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "service": "yt-dlp-http-service"
}
```

### GET /version
Get yt-dlp version.

**Response:**
```json
{
  "version": "2023.11.16",
  "service": "yt-dlp-http-service"
}
```

### POST /metadata
Get metadata for a URL without downloading.

**Request:**
```json
{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID"
}
```

**Response:** JSON object with video metadata (same as yt-dlp --dump-json)

### POST /download
Download media from URL.

**Request:**
```json
{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID",
  "itemIndex": 0
}
```

**Response:**
```json
{
  "filePath": "/downloads/video-title-id.mp4",
  "fileName": "video-title-id.mp4",
  "sizeBytes": 12345678
}
```

## Running Standalone

To run this service independently:

```bash
# Build image
docker build -t ytdlp-service .

# Run container
docker run -d \
  -p 8090:8090 \
  -v ./downloads:/downloads \
  -e DOWNLOAD_DIR=/downloads \
  ytdlp-service
```

## Development

### Requirements
- Python 3.11+
- Flask
- yt-dlp
- ffmpeg (for video processing)

### Running Locally
```bash
pip install -r requirements.txt
export DOWNLOAD_DIR=/tmp/downloads
python app.py
```

The service will be available at http://localhost:8090

## Testing

```bash
# Health check
curl http://localhost:8090/health

# Get version
curl http://localhost:8090/version

# Get metadata
curl -X POST http://localhost:8090/metadata \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'

# Download video
curl -X POST http://localhost:8090/download \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"}'
```

## Configuration

Environment variables:
- `DOWNLOAD_DIR` - Directory for downloaded files (default: `/downloads`)

## Architecture

```
┌───────────────────────────────────────┐
│  Flask HTTP Service (Port 8090)      │
├───────────────────────────────────────┤
│  POST /metadata  → yt-dlp --dump-json│
│  POST /download  → yt-dlp -o file    │
└───────────────────────────────────────┘
            ↓
        yt-dlp CLI
            ↓
      /downloads directory
```
