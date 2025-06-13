package com.tskrypko.streaming.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice(basePackages = "com.tskrypko.streaming.controller")
public class StreamingExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(StreamingExceptionHandler.class);

    @ExceptionHandler(StreamingRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleStreamingRuntimeException(StreamingRuntimeException ex) {
        logger.error("StreamingRuntimeException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getCode(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // Video related exceptions
    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVideoNotFoundException(VideoNotFoundException ex) {
        logger.warn("VideoNotFoundException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getCode(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(VideoAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleVideoAccessDeniedException(VideoAccessDeniedException ex) {
        logger.warn("VideoAccessDeniedException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getCode(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // Session related exceptions
    @ExceptionHandler(SessionAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSessionAccessDeniedException(SessionAccessDeniedException ex) {
        logger.warn("SessionAccessDeniedException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getCode(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(SessionGenerationException.class)
    public ResponseEntity<ErrorResponse> handleSessionGenerationException(SessionGenerationException ex) {
        logger.error("SessionGenerationException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getCode(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // Authentication exceptions
    @ExceptionHandler(UserIdHeaderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserIdHeaderNotFoundException(UserIdHeaderNotFoundException ex) {
        logger.warn("UserIdHeaderNotFoundException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ErrorCode.USER_ID_HEADER_MISSING, null);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Validation exceptions
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("IllegalArgumentException caught: {}", ex.getMessage(), ex);
        
        ErrorCode errorCode = determineErrorCodeForIllegalArgument(ex.getMessage());
        HttpStatus status = determineStatusForIllegalArgument(errorCode);
        
        ErrorResponse response = new ErrorResponse(ex.getMessage(), errorCode, null);
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.error("MethodArgumentNotValidException caught: {}", ex.getMessage(), ex);

        List<FieldResponse> fields = ex.getBindingResult().getFieldErrors().stream()
                .filter(fieldError -> fieldError.getDefaultMessage() != null)
                .map(fieldError -> new FieldResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                )).collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                "Request validation error", 
                ErrorCode.REQUEST_VALIDATION_ERROR, 
                fields
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        logger.error("IllegalStateException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(), 
                ErrorCode.INTERNAL_SERVER_ERROR, 
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Generic exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                "Internal server error",
                ErrorCode.INTERNAL_SERVER_ERROR,
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorCode determineErrorCodeForIllegalArgument(String message) {
        if (message == null) {
            return ErrorCode.REQUEST_VALIDATION_ERROR;
        }
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("video") && lowerMessage.contains("not found")) {
            return ErrorCode.VIDEO_NOT_FOUND;
        }
        if (lowerMessage.contains("session") && lowerMessage.contains("invalid")) {
            return ErrorCode.SESSION_NOT_FOUND;
        }
        if (lowerMessage.contains("quality") && lowerMessage.contains("not available")) {
            return ErrorCode.NO_ENCODED_QUALITIES;
        }
        if (lowerMessage.contains("parameter") || lowerMessage.contains("argument")) {
            return ErrorCode.INVALID_PARAMETER;
        }
        
        return ErrorCode.REQUEST_VALIDATION_ERROR;
    }

    private HttpStatus determineStatusForIllegalArgument(ErrorCode errorCode) {
        return switch (errorCode) {
            case VIDEO_NOT_FOUND, SESSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VIDEO_ACCESS_DENIED, SESSION_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case REQUEST_VALIDATION_ERROR, INVALID_PARAMETER, NO_ENCODED_QUALITIES -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
} 