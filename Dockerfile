FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install yt-dlp and ffmpeg
RUN apk add --no-cache python3 py3-pip ffmpeg && \
    pip3 install --no-cache-dir yt-dlp

# Copy the built jar
COPY --from=build /workspace/app/build/libs/*.jar app.jar

# Create downloads directory
RUN mkdir -p /tmp/downloads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
