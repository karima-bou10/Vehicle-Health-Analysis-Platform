package com.renault.vehicleanalysis.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HealthReportResponse(
        UUID reportId,
        UUID telemetryId,
        String vin,
        int healthScore,
        String status,
        Instant telemetryTimestamp,
        Instant analyzedAt,
        List<PenaltyResponse> penalties
) {
}
