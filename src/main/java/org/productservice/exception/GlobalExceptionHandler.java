package org.productservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.productservice.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ProductNotFoundException ex,
            HttpServletRequest request) {
        return build("PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {
        return build("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    @ExceptionHandler(InvalidSortFieldException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            InvalidSortFieldException ex, HttpServletRequest request) {
        return build("INVALID_SORT_FIELD", HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        return build( "CONFLICT", HttpStatus.CONFLICT, ex, request);
    }

    private ResponseEntity<ErrorResponse> build(String errorCode, HttpStatus status, Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError(errorCode);
        errorResponse.setStatus(status.value());
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());
        return ResponseEntity.status(status)
                .body(errorResponse);
    }
}
