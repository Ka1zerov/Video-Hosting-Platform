package com.tskrypko.upload.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice(basePackages = "com.tskrypko.upload.controller")
public class UploadExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UploadExceptionHandler.class);

    @ExceptionHandler(UploadRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleUploadRuntimeException(UploadRuntimeException ex) {
        logger.error("UploadRuntimeException caught: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getCode(), null);
        return new ResponseEntity<>(response, ex.getStatus());
    }

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
        if (lowerMessage.contains("file") && lowerMessage.contains("type")) {
            return ErrorCode.INVALID_FILE_TYPE;
        }
        if (lowerMessage.contains("size") && lowerMessage.contains("exceed")) {
            return ErrorCode.FILE_SIZE_EXCEEDED;
        }
        if (lowerMessage.contains("filename")) {
            return ErrorCode.REQUEST_VALIDATION_ERROR;
        }
        if (lowerMessage.contains("empty")) {
            return ErrorCode.REQUEST_VALIDATION_ERROR;
        }
        
        return ErrorCode.REQUEST_VALIDATION_ERROR;
    }

    private HttpStatus determineStatusForIllegalArgument(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_FILE_TYPE, FILE_SIZE_EXCEEDED, REQUEST_VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
} 