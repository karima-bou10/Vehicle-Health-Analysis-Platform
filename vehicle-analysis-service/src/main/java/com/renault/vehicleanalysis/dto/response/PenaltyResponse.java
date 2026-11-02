package com.renault.vehicleanalysis.dto.response;

public record PenaltyResponse(
        String category,
        int points,
        String reason
) {
}
