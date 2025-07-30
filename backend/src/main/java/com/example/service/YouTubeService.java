package com.example.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class YouTubeService {

    private static final String DOWNLOAD_PATH = "D:/Movies/%(title)s.%(ext)s";

    public void downloadWithLiveLogs(String url, String quality, SseEmitter emitter) {
        new Thread(() -> {
            try {
                // Create directory if needed
                File downloadDir = new File("D:/Movies");
                if (!downloadDir.exists()) downloadDir.mkdirs();

                // 🔍 Step 1: Get size of the format using `yt-dlp -F`
                String formatSelector = getFormatSelector(quality);

                ProcessBuilder probePb = new ProcessBuilder("yt-dlp", "-F", url);
                Process probeProcess = probePb.start();

                BufferedReader probeReader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()));
                String probeLine;
                String sizeInfo = null;

                Pattern pattern = Pattern.compile("^(\\d+).*?\\b(\\d{3,4}x\\d{3,4})\\b.*?\\b(\\d+(\\.\\d+)?[KM]iB)\\b");
                while ((probeLine = probeReader.readLine()) != null) {
                    // e.g., 137          mp4        1920x1080   1080p  500.2MiB ...
                    if (probeLine.contains("video") && probeLine.contains("audio")) continue;
                    Matcher matcher = pattern.matcher(probeLine);
                    if (matcher.find()) {
                        if (probeLine.toLowerCase().contains(quality.toLowerCase())) {
                            sizeInfo = matcher.group(3);
                            emitter.send("SIZE::" + quality.toUpperCase() + "::" + sizeInfo);
                            break;
                        }
                    }
                }

                // 🔽 Step 2: Download with live progress
                ProcessBuilder pb = new ProcessBuilder(
                        "yt-dlp",
                        "-f", formatSelector,
                        "-o", DOWNLOAD_PATH,
                        "--newline",
                        url
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                   if (line.startsWith("[download]")) {
    Pattern p = Pattern.compile("(\\d+\\.\\d+)%\\s+of\\s+(.*?)\\s+at\\s+(.*?)\\s+ETA\\s+(.*)");
    Matcher m = p.matcher(line);
    if (m.find()) {
        String percent = m.group(1);
        String size = m.group(2);
        String speed = m.group(3);
        String eta = m.group(4);

        emitter.send("PROGRESS::" + percent + "::" + size + "::" + speed + "::" + eta);
    }
                   }
else if (line.contains("Destination: ")) {
                        emitter.send("📥 " + line.trim());
                    }
                }

                int exitCode = process.waitFor();
                emitter.send("✅ Download completed with exit code " + exitCode);
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send("❌ Error: " + e.getMessage());
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();
    }

    private String getFormatSelector(String quality) {
        return switch (quality.toLowerCase()) {
            case "8k" -> "bestvideo[height=4320]+bestaudio";
            case "4k" -> "bestvideo[height=2160]+bestaudio";
            case "1080p" -> "bestvideo[height=1080]+bestaudio";
            case "720p" -> "bestvideo[height=720]+bestaudio";
            case "480p" -> "bestvideo[height=480]+bestaudio";
            case "360p" -> "bestvideo[height=360]+bestaudio";
            default -> "bestvideo+bestaudio";
        };
    }
}
