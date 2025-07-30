package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.*;
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
        qualityFormatMap.put("best", "best");
    }

    public void downloadWithLiveLogs(String url, String formatLabel, SseEmitter emitter) {
        new Thread(() -> {
            String fileName = "video_" + UUID.randomUUID() + ".mp4";
            String outputPath = "downloads/" + fileName;
            String format = qualityFormatMap.getOrDefault(formatLabel.toLowerCase(), "best");

            List<String> command = Arrays.asList(
                    "yt-dlp",
                    "-f", format,
                    "--merge-output-format", "mp4",
                    "-o", outputPath,
                    "--newline",
                    url
            );

            // ✅ Print the full command for debugging
            System.out.println("Running yt-dlp command:");
            command.forEach(System.out::println);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            try {
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                Pattern pattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)% of ~?(\\d+\\.\\d+)?\\w* at ([\\d\\.\\w/]+) ETA ([\\d:\\.]+)");

                while ((line = reader.readLine()) != null) {
                    System.out.println("yt-dlp: " + line);

                    // ✅ Send full logs to frontend too (for debugging)
                    emitter.send(SseEmitter.event().data("📄 " + line));

                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String percent = matcher.group(1);
                        String totalMB = matcher.group(2) != null ? matcher.group(2) : "";
                        String speed = matcher.group(3);
                        String eta = matcher.group(4);

                        String progress = String.format("📥 %s%% downloaded (%s MB) at %s, ETA %s",
                                percent, totalMB, speed, eta);

                        emitter.send(SseEmitter.event().data(progress));
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
                try {
                    emitter.send(SseEmitter.event().data("❌ Error occurred: " + e.getMessage()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                emitter.completeWithError(e);
            }
        }).start();
    }
}
