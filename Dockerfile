# Stage 1: Build React frontend
FROM node:18 AS frontend-builder

WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build


# Stage 2: Build Spring Boot backend
FROM eclipse-temurin:17-jdk as backend-builder

WORKDIR /app

# Install tools
RUN apt-get update && \
    apt-get install -y maven python3 curl ffmpeg && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean

# Copy backend code and frontend build output
COPY backend/ ./backend/
COPY --from=frontend-builder /frontend/build ./backend/src/main/resources/static/

WORKDIR /app/backend
RUN mvn clean package -DskipTests


# Stage 3: Final runtime image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install yt-dlp and ffmpeg again (for runtime)
RUN apt-get update && \
    apt-get install -y python3 curl ffmpeg && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean

COPY --from=backend-builder /app/backend/target/youtube-downloader-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
