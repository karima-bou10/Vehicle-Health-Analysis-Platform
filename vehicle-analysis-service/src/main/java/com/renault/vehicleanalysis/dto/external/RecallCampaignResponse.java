package com.renault.vehicleanalysis.dto.external;

import com.renault.vehicleanalysis.enums.RecallSeverity;
import com.renault.vehicleanalysis.enums.RecallStatus;

public record RecallCampaignResponse(
        String campaignId,
        String description,
        RecallSeverity severity,
        RecallStatus status
) {
}