package com.tskrypko.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    
    private String sessionId;
    private LocalDateTime expiresAt;
    private Long videoId;
    private String userId;
    private Boolean isActive;
} 