package com.tskrypko.streaming.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewSessionRequest {
    
    @NotNull(message = "Session ID is required")
    private String sessionId;
    
    private Long currentPosition; // in seconds
    private Long watchDuration; // in seconds
    private String quality;
    private Boolean isComplete = false;
} 