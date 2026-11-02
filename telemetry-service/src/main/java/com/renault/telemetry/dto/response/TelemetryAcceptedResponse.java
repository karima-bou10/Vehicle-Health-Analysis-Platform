package com.renault.telemetry.dto.response;

import com.renault.telemetry.entity.TelemetryStatus;

import java.util.UUID;

public record TelemetryAcceptedResponse(
        UUID telemetryId,
        String vin,
        TelemetryStatus status,
        String message
) {
}
