package com.renault.telemetry.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TirePressureRequest(

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("5.0")
        Double frontLeft,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("5.0")
        Double frontRight,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("5.0")
        Double rearLeft,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("5.0")
        Double rearRight
) {
}
