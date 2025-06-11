package com.tskrypko.upload.exception;

import org.springframework.http.HttpStatus;

public class VideoNotFoundException extends UploadRuntimeException {
    
    public VideoNotFoundException(Long videoId) {
        super("Video not found with ID: " + videoId, ErrorCode.VIDEO_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
    
    public VideoNotFoundException(String message) {
        super(message, ErrorCode.VIDEO_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
} 