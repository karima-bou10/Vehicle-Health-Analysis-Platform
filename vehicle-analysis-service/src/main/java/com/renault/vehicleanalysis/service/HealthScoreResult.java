package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.enums.HealthStatus;

import java.util.List;

public record HealthScoreResult(
        int score,
        HealthStatus status,
        List<PenaltyResult> penalties
) {
}
