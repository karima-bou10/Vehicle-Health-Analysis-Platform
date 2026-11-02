package com.renault.vehicleanalysis.dto.response;

import java.util.List;

public record VehicleAlertsResponse(
        String vin,
        int alertCount,
        List<AlertResponse> alerts
) {
}