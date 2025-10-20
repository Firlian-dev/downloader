FROM python:3.11-alpine

# Install ffmpeg
RUN apk add --no-cache ffmpeg curl

# Install yt-dlp
RUN pip3 install --no-cache-dir yt-dlp

# Create working directory
WORKDIR /app

# Copy service files
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY app.py .

# Create downloads directory
RUN mkdir -p /downloads

# Expose port for HTTP service
EXPOSE 8090

# Run the service
CMD ["python3", "app.py"]
