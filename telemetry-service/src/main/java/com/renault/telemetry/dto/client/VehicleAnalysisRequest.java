package com.renault.telemetry.dto.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VehicleAnalysisRequest(
        UUID telemetryId,
        String vin,
        Instant timestamp,
        Long mileageKm,
        TirePressureData tirePressure,
        Integer batteryLevelPercent,
        Double engineTemperatureCelsius,
        Integer fuelLevelPercent,
        List<String> errorCodes
) {
}
