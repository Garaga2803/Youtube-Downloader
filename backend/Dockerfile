FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get update &&     apt-get install -y maven python3 curl ffmpeg &&     curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp &&     chmod a+rx /usr/local/bin/yt-dlp &&     apt-get clean

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/youtube-downloader-0.0.1-SNAPSHOT.jar"]