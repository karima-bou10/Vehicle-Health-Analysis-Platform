package com.renault.vehicleanalysis.dto.response;

import java.util.UUID;

public record VehicleAnalysisResponse(
        UUID analysisId,
        String vin,
        int healthScore,
        String status,
        int alertCount
) {
}
