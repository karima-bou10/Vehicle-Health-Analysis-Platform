package com.renault.vehicleanalysis.dto.response;

public record ValidationError(
        String field,
        String message
) {
}
