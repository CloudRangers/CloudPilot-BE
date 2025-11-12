package com.cloudrangers.cloudpilot.common;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private String errorCode; // null이면 성공
    private String message;   // 성공 시 "OK"
    private T result;

    public static <T> ApiResponse<T> ok(T result) {
        return ApiResponse.<T>builder()
                .errorCode(null)
                .message("OK")
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .errorCode(errorCode)
                .message(message)
                .result(null)
                .build();
    }
}
