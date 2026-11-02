package com.renault.vehicleanalysis.client;

import com.renault.vehicleanalysis.dto.external.RecallProviderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RecallProviderClient {

    private final RestClient restClient;

    public RecallProviderClient(
            RestClient recallProviderRestClient) {
        this.restClient = recallProviderRestClient;
    }

    public RecallProviderResponse getRecalls(String vin) {

        return restClient.get()
                .uri("/api/v1/recalls/{vin}", vin)
                .retrieve()
                .body(RecallProviderResponse.class);
    }
}