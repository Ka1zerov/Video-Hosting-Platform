package com.tskrypko.streaming.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "view_sessions")
@NoArgsConstructor
public class ViewSession extends BaseEntity {

    @NotNull(message = "Video ID is required")
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_id")
    private String userId; // Can be null for anonymous users

    @Column(name = "session_id", nullable = false)
    private String sessionId; // Unique session identifier

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "watch_duration")
    private Long watchDuration = 0L; // Duration in seconds

    @Column(name = "max_position")
    private Long maxPosition = 0L; // Maximum position reached in seconds

    @Enumerated(EnumType.STRING)
    @Column(name = "quality")
    private StreamQuality quality = StreamQuality.AUTO;

    @Column(name = "is_complete")
    private Boolean isComplete = false;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public ViewSession(Long videoId, String sessionId, String ipAddress) {
        this.videoId = videoId;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        LocalDateTime now = LocalDateTime.now();
        this.startedAt = now;
        this.lastHeartbeat = now;
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (lastHeartbeat == null) {
            lastHeartbeat = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "ViewSession{" +
                "id=" + getId() +
                ", videoId=" + videoId +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", watchDuration=" + watchDuration +
                ", quality=" + quality +
                ", startedAt=" + startedAt +
                '}';
    }
} 