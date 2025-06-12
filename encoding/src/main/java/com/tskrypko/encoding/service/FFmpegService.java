package com.tskrypko.encoding.service;

import lombok.Getter;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Service
@Getter
public class FFmpegService {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegService.class);

    @Value("${ffmpeg.path:/usr/bin/ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:/usr/bin/ffprobe}")
    private String ffprobePath;

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    @PostConstruct
    public void init() throws IOException {
        logger.info("Initializing FFmpeg service with paths: ffmpeg={}, ffprobe={}", ffmpegPath, ffprobePath);
        
        try {
            this.ffmpeg = new FFmpeg(ffmpegPath);
            this.ffprobe = new FFprobe(ffprobePath);
            
            logger.info("FFmpeg version: {}", ffmpeg.version());
            logger.info("FFprobe version: {}", ffprobe.version());
            
        } catch (IOException e) {
            logger.error("Failed to initialize FFmpeg: {}", e.getMessage(), e);
            throw new RuntimeException("FFmpeg initialization failed", e);
        }
    }
} 