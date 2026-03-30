package org.voice.membership.dtos;

import lombok.Builder;

/**
 * Standard API envelope for all REST responses.
 */
@Builder
public record ApiResponse<T>(String status, String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .data(data)
                .build();
    }
}
