package com.renault.vehicleanalysis.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String type,
        String severity,
        String message,
        String measuredValue,
        String thresholdValue,
        Instant createdAt
) {
}