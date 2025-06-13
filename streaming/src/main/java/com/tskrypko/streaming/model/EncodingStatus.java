package com.tskrypko.streaming.model;

public enum EncodingStatus {
    PENDING,         // Encoding not started yet
    IN_PROGRESS,     // Currently encoding
    COMPLETED,       // Encoding completed successfully
    FAILED,          // Encoding failed
    CANCELLED        // Encoding was cancelled
} 