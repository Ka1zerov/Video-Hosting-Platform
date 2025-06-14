package com.tskrypko.metadata.exception;

import java.util.UUID;

public class VideoNotFoundException extends RuntimeException {
    
    public VideoNotFoundException(UUID videoId) {
        super("Video not found with id: " + videoId);
    }
    
    public VideoNotFoundException(String message) {
        super(message);
    }
} 