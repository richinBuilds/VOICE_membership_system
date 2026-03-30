package org.voice.membership.config;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.voice.membership.dtos.ApiErrorData;
import org.voice.membership.dtos.ApiResponse;
import org.voice.membership.exceptions.BadRequestException;
import org.voice.membership.exceptions.ResourceNotFoundException;
import org.voice.membership.exceptions.UnauthorizedException;

import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiErrorData>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiErrorData data = ApiErrorData.builder()
                .errorCode("VALIDATION_ERROR")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", data));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ApiErrorData>> handleConstraintViolation(ConstraintViolationException ex) {
        ApiErrorData data = ApiErrorData.builder()
                .errorCode("VALIDATION_ERROR")
                .fieldErrors(Map.of("constraint", ex.getMessage()))
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", data));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiErrorData>> handleNotFound(ResourceNotFoundException ex) {
        ApiErrorData data = ApiErrorData.builder().errorCode("NOT_FOUND").fieldErrors(Map.of()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage(), data));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<ApiErrorData>> handleUnauthorized(UnauthorizedException ex) {
        ApiErrorData data = ApiErrorData.builder().errorCode("UNAUTHORIZED").fieldErrors(Map.of()).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage(), data));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ApiErrorData>> handleBadRequest(BadRequestException ex) {
        ApiErrorData data = ApiErrorData.builder().errorCode("BAD_REQUEST").fieldErrors(Map.of()).build();
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), data));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiErrorData>> handleGeneric(Exception ex) {
        ApiErrorData data = ApiErrorData.builder().errorCode("INTERNAL_ERROR").fieldErrors(Map.of()).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", data));
    }
}
