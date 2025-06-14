package com.tskrypko.streaming.controller;

import com.tskrypko.streaming.dto.ViewSessionRequest;
import com.tskrypko.streaming.model.ViewSession;
import com.tskrypko.streaming.service.ViewSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/streaming/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ViewSessionController {

    private final ViewSessionService viewSessionService;

    /**
     * Start a new viewing session
     */
    @PostMapping("/start")
    public ResponseEntity<ViewSession> startViewSession(
            @RequestParam UUID videoId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        
        log.info("Starting view session for video: {} by user: {}", videoId, userId);
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        ViewSession session = viewSessionService.startViewSession(videoId, userId, ipAddress, userAgent);
        
        return ResponseEntity.ok(session);
    }

    /**
     * Update viewing session progress (heartbeat)
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<String> updateViewSession(@Valid @RequestBody ViewSessionRequest request) {
        
        log.debug("Heartbeat for session: {}", request.getSessionId());
        
        viewSessionService.updateViewSession(request);
        return ResponseEntity.ok("Session updated");
    }

    /**
     * End viewing session
     */
    @PostMapping("/end")
    public ResponseEntity<String> endViewSession(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "false") boolean isComplete) {
        
        log.info("Ending view session: {} (complete: {})", sessionId, isComplete);
        
        viewSessionService.endViewSession(sessionId, isComplete);
        return ResponseEntity.ok("Session ended");
    }

    /**
     * Get active session by session ID
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ViewSession> getActiveSession(@PathVariable String sessionId) {
        
        log.debug("Getting active session: {}", sessionId);
        
        Optional<ViewSession> session = viewSessionService.getActiveSession(sessionId);
        
        if (session.isPresent()) {
            return ResponseEntity.ok(session.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get viewing sessions for a video
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<Page<ViewSession>> getVideoSessions(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting sessions for video: {}, page: {}, size: {}", videoId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ViewSession> sessions = viewSessionService.getVideoSessions(videoId, pageable);
        
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get viewing sessions for a user (with access control)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ViewSession>> getUserSessions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting sessions for user: {}, page: {}, size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ViewSession> sessions = viewSessionService.getUserSessions(userId, pageable);
        
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get analytics for a video
     */
    @GetMapping("/analytics/{videoId}")
    public ResponseEntity<ViewSessionService.VideoAnalytics> getVideoAnalytics(@PathVariable UUID videoId) {
        
        log.info("Getting analytics for video: {}", videoId);
        
        ViewSessionService.VideoAnalytics analytics = viewSessionService.getVideoAnalytics(videoId);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get all active sessions (admin endpoint)
     */
    @GetMapping("/active")
    public ResponseEntity<List<ViewSession>> getActiveSessions() {
        
        log.info("Getting all active sessions");
        
        List<ViewSession> sessions = viewSessionService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get watch time for a video
     */
    @GetMapping("/watch-time/{videoId}")
    public ResponseEntity<Long> getTotalWatchTime(@PathVariable UUID videoId) {
        
        log.debug("Getting total watch time for video: {}", videoId);
        
        Long watchTime = viewSessionService.getTotalWatchTime(videoId);
        return ResponseEntity.ok(watchTime);
    }

    /**
     * Get unique viewers count for a video
     */
    @GetMapping("/viewers/{videoId}")
    public ResponseEntity<Long> getUniqueViewersCount(@PathVariable UUID videoId) {
        
        log.debug("Getting unique viewers count for video: {}", videoId);
        
        Long viewersCount = viewSessionService.getUniqueViewersCount(videoId);
        return ResponseEntity.ok(viewersCount);
    }

    /**
     * Get completion rate for a video
     */
    @GetMapping("/completion-rate/{videoId}")
    public ResponseEntity<Double> getCompletionRate(@PathVariable UUID videoId) {
        
        log.debug("Getting completion rate for video: {}", videoId);
        
        Double completionRate = viewSessionService.getCompletionRate(videoId);
        return ResponseEntity.ok(completionRate);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 