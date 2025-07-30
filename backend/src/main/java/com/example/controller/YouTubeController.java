package com.example.controller;

import com.example.service.YouTubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/youtube")
@CrossOrigin(origins = "*")
public class YouTubeController {

    @Autowired
    private YouTubeService youtubeService;

    @GetMapping(path = "/download/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDownload(@RequestParam String url, @RequestParam(defaultValue = "best") String format) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        youtubeService.downloadWithLiveLogs(url, format, emitter);
        return emitter;
    }
}