package com.renault.vehicleanalysis.client;

import com.renault.vehicleanalysis.dto.external.RecallProviderResponse;
import com.renault.vehicleanalysis.dto.request.TirePressureData;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.dto.response.VehicleAnalysisResponse;
import com.renault.vehicleanalysis.enums.RecallSeverity;
import com.renault.vehicleanalysis.enums.RecallStatus;
import com.renault.vehicleanalysis.service.VehicleAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.wiremock.spring.EnableWireMock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "external-services.recall-provider.base-url=${wiremock.server.baseUrl}"
})
@EnableWireMock
@Transactional
class RecallProviderClientIntegrationTest {

    @Autowired
    private RecallProviderClient recallProviderClient;

    @Autowired
    private VehicleAnalysisService vehicleAnalysisService;

    @Test
    void shouldRetrieveRecallCampaignsForVehicle() {

        String vin = "VF1ABC12345678901";

        stubFor(get(urlEqualTo(
                "/api/v1/recalls/" + vin
        ))
                .willReturn(okJson("""
                    {
                      "vin": "VF1ABC12345678901",
                      "recalls": [
                        {
                          "campaignId": "RC-2026-015",
                          "description":
                            "Battery control unit inspection",
                          "severity": "HIGH",
                          "status": "OPEN"
                        }
                      ]
                    }
                    """)));

        RecallProviderResponse response =
                recallProviderClient.getRecalls(vin);

        assertThat(response).isNotNull();
        assertThat(response.vin()).isEqualTo(vin);
        assertThat(response.recalls()).hasSize(1);

        assertThat(response.recalls().getFirst().campaignId())
                .isEqualTo("RC-2026-015");

        assertThat(response.recalls().getFirst().severity())
                .isEqualTo(RecallSeverity.HIGH);

        assertThat(response.recalls().getFirst().status())
                .isEqualTo(RecallStatus.OPEN);

        verify(getRequestedFor(
                urlEqualTo("/api/v1/recalls/" + vin)
        ));
    }

    @Test
    void shouldReturnEmptyListWhenVehicleHasNoRecall() {

        String vin = "VF1NOACTIVE000001";

        stubFor(get(urlEqualTo(
                "/api/v1/recalls/" + vin
        ))
                .willReturn(okJson("""
                {
                  "vin": "VF1NOACTIVE000001",
                  "recalls": []
                }
                """)));

        RecallProviderResponse response =
                recallProviderClient.getRecalls(vin);

        assertThat(response.recalls()).isEmpty();
    }

    @Test
    void shouldContinueAnalysisWhenRecallProviderFails() {

        String vin = "VF1ABC12345678902";

        stubFor(get(urlEqualTo(
                "/api/v1/recalls/" + vin
        ))
                .willReturn(serverError()));

        VehicleAnalysisRequest request =
                createRequestWithBatteryLevel(vin, 80);

        VehicleAnalysisResponse response =
                vehicleAnalysisService.analyze(request);

        assertThat(response.healthScore()).isEqualTo(100);
        assertThat(response.status()).isEqualTo("HEALTHY");
    }

    @Test
    void shouldIncludeRecallInAnalysis() {

        String vin = "VF1ABC12345678903";

        stubFor(get(urlEqualTo(
                "/api/v1/recalls/" + vin
        ))
                .willReturn(okJson("""
                {
                  "vin": "VF1ABC12345678903",
                  "recalls": [
                    {
                      "campaignId": "RC-2026-015",
                      "description":
                        "Battery control unit inspection",
                      "severity": "HIGH",
                      "status": "OPEN"
                    }
                  ]
                }
                """)));

        VehicleAnalysisRequest request =
                createRequestWithBatteryLevel(vin, 35);

        VehicleAnalysisResponse response =
                vehicleAnalysisService.analyze(request);

        assertThat(response.healthScore()).isEqualTo(80);
        assertThat(response.alertCount()).isEqualTo(2);

        verify(1, getRequestedFor(
                urlEqualTo("/api/v1/recalls/" + vin)
        ));
    }

    // Méthodes utilitaires

    private VehicleAnalysisRequest createRequestWithBatteryLevel(
            String vin,
            int batteryLevel) {

        return new VehicleAnalysisRequest(
                UUID.randomUUID(),
                vin,
                Instant.now(),
                25_000L,
                new TirePressureData(
                        2.3,
                        2.2,
                        2.4,
                        2.1
                ),
                batteryLevel,
                90.0,
                60,
                List.of()
        );
    }
}