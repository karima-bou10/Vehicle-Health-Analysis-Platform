package com.renault.telemetry.dto.response;

public record ValidationError(
        String field,
        String message
) {
}
