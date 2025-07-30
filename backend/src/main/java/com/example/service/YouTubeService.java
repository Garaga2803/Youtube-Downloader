package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.UUID;
import java.util.regex.*;

@Service
public class YouTubeService {

    public void downloadWithLiveLogs(String url, String formatCode, SseEmitter emitter) {
        new Thread(() -> {
            String fileName = "video_" + UUID.randomUUID() + ".mp4";
            String outputPath = "downloads/" + fileName;

            ProcessBuilder builder = new ProcessBuilder(
                    "yt-dlp",
                    "-f", formatCode,
                    "--merge-output-format", "mp4",
                    "-o", outputPath,
                    "--newline",
                    url
            );
            builder.redirectErrorStream(true);

            try {
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                Pattern pattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)% of ~?(\\d+\\.\\d+)?\\w* at ([\\d\\.\\w/]+) ETA ([\\d:\\.]+)");

                while ((line = reader.readLine()) != null) {
                    System.out.println("yt-dlp: " + line);

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
