package com.duelo64.backend.shared.error;

public record ApiErrorResponse(
        String code,
        String message
) {
}