package com.nexora.sport.dto;

import java.util.Map;

public record ApiResponse<T>(boolean success, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public static ApiResponse<Map<String, String>> fieldErrors(Map<String, String> errors) {
        return new ApiResponse<>(false, "Error de validacion", errors);
    }
}
