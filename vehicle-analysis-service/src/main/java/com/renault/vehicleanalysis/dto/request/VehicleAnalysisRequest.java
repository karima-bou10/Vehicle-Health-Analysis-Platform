package com.renault.vehicleanalysis.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VehicleAnalysisRequest(

        @NotNull
        UUID telemetryId,

        @NotBlank
        String vin,

        @NotNull
        @PastOrPresent
        Instant timestamp,

        @NotNull
        @PositiveOrZero
        Long mileageKm,

        @NotNull
        TirePressureData tirePressure,

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
