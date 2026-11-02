package com.renault.telemetry.dto.client;

public record TirePressureData(
        Double frontLeft,
        Double frontRight,
        Double rearLeft,
        Double rearRight
) {
}
