package com.tskrypko.streaming.model;

public enum VideoStatus {
    UPLOADED,        // Video uploaded but not processed
    PROCESSING,      // Video being processed/encoded
    ENCODED,         // Video encoded successfully
    READY,           // Video ready for streaming
    FAILED,          // Processing/encoding failed
    DELETED,         // Video deleted
    ARCHIVED         // Video archived (not available for streaming)
} 