package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.enums.PenaltyCategory;

public record PenaltyResult(
        PenaltyCategory category,
        int points,
        String reason
) {
}
