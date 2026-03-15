package iuh.fit.notificationservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(
            AppException ex, HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();
        log.error("AppException: {} - {} at {}", errorCode.getCode(), ex.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(Map.of(
                        "code",      errorCode.getCode(),
                        "message",   ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);

        return ResponseEntity.badRequest().body(Map.of(
                "code",      ErrorCode.VALIDATION_FAILED.getCode(),
                "message",   "Validation failed",
                "errors",    errors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected exception at {}: ", request.getRequestURI(), ex);

        return ResponseEntity.internalServerError().body(Map.of(
                "code",      ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                "message",   "An unexpected error occurred",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}