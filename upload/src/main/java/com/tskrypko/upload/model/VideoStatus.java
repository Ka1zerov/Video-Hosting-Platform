package com.tskrypko.upload.model;

public enum VideoStatus {
    UPLOADED,        // Video uploaded
    PROCESSING,      // Video being processed
    ENCODED,         // Video encoded
    READY,           // Video ready for viewing
    FAILED,          // Processing failed
    DELETED          // Video deleted
} 