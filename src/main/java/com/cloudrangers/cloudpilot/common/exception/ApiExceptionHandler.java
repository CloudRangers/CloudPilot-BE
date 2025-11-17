package com.cloudrangers.cloudpilot.common.exception;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCatalogNotFound(CatalogNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CatalogConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleCatalogConflict(CatalogConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }
}
