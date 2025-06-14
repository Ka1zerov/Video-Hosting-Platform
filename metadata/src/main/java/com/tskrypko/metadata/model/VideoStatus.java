package com.tskrypko.metadata.model;

public enum VideoStatus {
    UPLOADED,        // Video uploaded but not processed
    PROCESSING,      // Video being processed/encoded
    READY,           // Video ready for streaming
    FAILED,          // Processing/encoding failed
    DELETED          // Video deleted
} 