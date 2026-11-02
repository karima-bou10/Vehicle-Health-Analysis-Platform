package com.renault.telemetry.dto.request;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record CreateTelemetryRequest(

        @NotBlank
        @Size(min = 17, max = 17)
        String vin,

        @NotNull
        @PastOrPresent
        Instant timestamp,

        @NotNull
        @PositiveOrZero
        Long mileageKm,

        @NotNull
        TirePressureRequest tirePressure,

        @NotNull
        @Min(0)
        @Max(100)
        Integer batteryLevelPercent,

        @NotNull
        Double engineTemperatureCelsius,

        @NotNull
        @Min(0)
        @Max(100)
        Integer fuelLevelPercent,

        List<String> errorCodes
) {
}
