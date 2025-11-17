package com.cloudrangers.cloudpilot.exception;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CovigatorException.class)
    public ApiResponse<Object> handleCovigatorException(CovigatorException ex) {

        log.error("❌ Custom Exception 발생: {}", ex.getMessage());

        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleGeneralException(Exception ex) {

        log.error("🔥 Unexpected System Error: ", ex);

        return ApiResponse.fail("서버 내부 오류가 발생했습니다.");
    }
}
