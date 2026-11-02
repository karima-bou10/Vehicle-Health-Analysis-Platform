package com.renault.vehicleanalysis.dto.external;

import java.util.List;

public record RecallProviderResponse(
        String vin,
        List<RecallCampaignResponse> recalls
) {
}
