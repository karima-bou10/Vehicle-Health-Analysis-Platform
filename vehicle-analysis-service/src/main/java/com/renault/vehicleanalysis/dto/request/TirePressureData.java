package com.renault.vehicleanalysis.dto.request;

import jakarta.validation.constraints.NotNull;

public record TirePressureData(
        @NotNull Double frontLeft,
        @NotNull Double frontRight,
        @NotNull Double rearLeft,
        @NotNull Double rearRight
) {
}