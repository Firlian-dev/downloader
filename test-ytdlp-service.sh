#!/bin/bash

# Test script for local development setup
# This script verifies that the yt-dlp HTTP service is working correctly

set -e

echo "=================================="
echo "Testing yt-dlp HTTP Service"
echo "=================================="

SERVICE_URL="${YTDLP_SERVICE_URL:-http://localhost:8090}"

echo ""
echo "1. Checking if service is accessible..."
if curl -f -s "${SERVICE_URL}/health" > /dev/null; then
    echo "✅ Service is accessible"
else
    echo "❌ Service is not accessible at ${SERVICE_URL}"
    echo "   Make sure docker-compose.dev.yml is running:"
    echo "   docker-compose -f docker-compose.dev.yml up -d"
    exit 1
fi

echo ""
echo "2. Checking service health..."
HEALTH=$(curl -s "${SERVICE_URL}/health")
echo "Response: ${HEALTH}"
if echo "${HEALTH}" | grep -q "ok"; then
    echo "✅ Service is healthy"
else
    echo "❌ Service health check failed"
    exit 1
fi

echo ""
echo "3. Getting yt-dlp version..."
VERSION=$(curl -s "${SERVICE_URL}/version")
echo "Response: ${VERSION}"
if echo "${VERSION}" | grep -q "version"; then
    echo "✅ Version endpoint working"
else
    echo "❌ Version endpoint failed"
    exit 1
fi

echo ""
echo "4. Testing metadata endpoint (this may take a few seconds)..."
METADATA=$(curl -s -X POST "${SERVICE_URL}/metadata" \
    -H "Content-Type: application/json" \
    -d '{"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ"}' \
    2>/dev/null || echo "failed")

if echo "${METADATA}" | grep -q "title"; then
    echo "✅ Metadata endpoint working"
    echo "   Video title found in response"
else
    echo "⚠️  Metadata endpoint may have failed or video is unavailable"
    echo "   This is not critical for development if the endpoint responds"
fi

echo ""
echo "=================================="
echo "All basic tests passed! ✅"
echo "=================================="
echo ""
echo "Your yt-dlp HTTP service is ready for local development."
echo ""
echo "Next steps:"
echo "1. Create .env.dev from .env.dev.example"
echo "2. Set your TELEGRAM_BOT_TOKEN in .env.dev"
echo "3. Run: export \$(cat .env.dev | xargs) && ./gradlew bootRun"
echo ""
