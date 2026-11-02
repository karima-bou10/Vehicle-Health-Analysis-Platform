package com.renault.telemetry.client;

import com.renault.telemetry.dto.client.VehicleAnalysisRequest;
import com.renault.telemetry.dto.response.VehicleAnalysisResponse;
import com.renault.telemetry.service.AnalysisDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VehicleAnalysisClient {

    private final RestClient restClient;

    public VehicleAnalysisClient(
            RestClient vehicleAnalysisRestClient) {
        this.restClient = vehicleAnalysisRestClient;
    }

    public VehicleAnalysisResponse requestAnalysis(
            VehicleAnalysisRequest request) {

        return restClient.post()
                .uri("/internal/api/v1/analyses")
                .body(request)
                .retrieve()
                .body(VehicleAnalysisResponse.class);
    }
}
