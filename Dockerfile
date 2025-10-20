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

# Copy the built jar
COPY --from=build /workspace/app/build/libs/*.jar app.jar

# Create downloads directory
RUN mkdir -p /app/downloads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
