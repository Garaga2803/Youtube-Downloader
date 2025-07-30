package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.*;

@Service
public class YouTubeService {

    private final Map<String, String> qualityFormatMap = new HashMap<>();

    public YouTubeService() {
        qualityFormatMap.put("8k", "bestvideo[height<=4320]+bestaudio/best");
        qualityFormatMap.put("4k", "bestvideo[height<=2160]+bestaudio/best");
        qualityFormatMap.put("1440p", "bestvideo[height<=1440]+bestaudio/best");
        qualityFormatMap.put("1080p", "bestvideo[height<=1080]+bestaudio/best");
        qualityFormatMap.put("720p", "bestvideo[height<=720]+bestaudio/best");
        qualityFormatMap.put("480p", "bestvideo[height<=480]+bestaudio/best");
        qualityFormatMap.put("360p", "bestvideo[height<=360]+bestaudio/best");
        qualityFormatMap.put("240p", "bestvideo[height<=240]+bestaudio/best");
        qualityFormatMap.put("best", "best"); // fallback
    }

    public SseEmitter downloadVideoWithProgress(String url, String formatLabel) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        new Thread(() -> {
            String fileName = "video_" + UUID.randomUUID() + ".mp4";

            // Use mapped format or fallback to "best"
            String format = qualityFormatMap.getOrDefault(formatLabel.toLowerCase(), "best");

            ProcessBuilder builder = new ProcessBuilder(
                    "yt-dlp",
                    "-f", format,
                    "-o", "downloads/" + fileName,
                    "--newline",
                    url
            );

            builder.redirectErrorStream(true);

            try {
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)% of ~?(\\d+\\.\\d+)?\\w* at ([\\d\\.\\w/]+) ETA ([\\d:\\.]+)");

                while ((line = reader.readLine()) != null) {
                    System.out.println("yt-dlp: " + line);

                    Matcher matcher = progressPattern.matcher(line);
                    if (matcher.find()) {
                        String progress = matcher.group(1);
                        String totalSize = matcher.group(2) != null ? matcher.group(2) : "";
                        String speed = matcher.group(3);
                        String eta = matcher.group(4);

                        String formatted = String.format("📥 %s%% downloaded (%s MB) at %s, ETA %s", progress, totalSize, speed, eta);
                        emitter.send(SseEmitter.event().data(formatted));
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    emitter.send(SseEmitter.event().data("✅ Download completed. Ready to download file:" + fileName));
                } else {
                    emitter.send(SseEmitter.event().data("❌ Download failed with exit code: " + exitCode));
                }

                emitter.complete();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                try {
                    emitter.send(SseEmitter.event().data("❌ Error occurred: " + e.getMessage()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
