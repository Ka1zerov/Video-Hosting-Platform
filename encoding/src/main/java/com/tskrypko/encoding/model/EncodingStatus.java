package com.tskrypko.encoding.model;

public enum EncodingStatus {
    PENDING,      // Job created, waiting to be processed
    PROCESSING,   // Job is being processed
    COMPLETED,    // Job completed successfully
    FAILED,       // Job failed
    RETRY         // Job failed but will be retried
} 