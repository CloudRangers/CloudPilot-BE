package com.cloudrangers.cloudpilot.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.of(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.of(false, null, message);
    }
}
