package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.external.RecallCampaignResponse;
import com.renault.vehicleanalysis.dto.request.TirePressureData;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.entity.*;
import com.renault.vehicleanalysis.enums.AlertSeverity;
import com.renault.vehicleanalysis.enums.AlertType;
import com.renault.vehicleanalysis.enums.RecallSeverity;
import com.renault.vehicleanalysis.enums.RecallStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AlertGeneratorTest {

    private AlertGenerator alertGenerator;

    @BeforeEach
    void setUp() {
        alertGenerator = new AlertGenerator();
    }

    @Test
    void shouldGenerateCriticalEngineTemperatureAlert() {

        VehicleAnalysisRequest request = createRequestWithEngineTemperature(120.0);

        List<Alert> alerts = alertGenerator.generate(request, List.of());

        assertThat(alerts)
                .anySatisfy(alert -> {
                    assertThat(alert.getType())
                            .isEqualTo(
                                    AlertType.ENGINE_TEMPERATURE
                            );

                    assertThat(alert.getSeverity())
                            .isEqualTo(AlertSeverity.CRITICAL);

                    assertThat(alert.getMeasuredValue())
                            .isEqualTo("120.0 °C");
                });
    }

    @Test
    void shouldGenerateNoAlertsForHealthyTelemetry() {

        VehicleAnalysisRequest request = createHealthyRequest();

        List<Alert> alerts = alertGenerator.generate(request, List.of());

        assertThat(alerts).isEmpty();
    }

    @Test
    void shouldGenerateAlertForOpenRecall() {

        VehicleAnalysisRequest request =
                createHealthyRequest();

        RecallCampaignResponse recall =
                new RecallCampaignResponse(
                        "RC-2026-015",
                        "Battery control unit inspection",
                        RecallSeverity.HIGH,
                        RecallStatus.OPEN
                );

        List<Alert> alerts =
                alertGenerator.generate(
                        request,
                        List.of(recall)
                );

        assertThat(alerts).hasSize(1);

        Alert alert = alerts.getFirst();

        assertThat(alert.getType())
                .isEqualTo(AlertType.ACTIVE_RECALL);

        assertThat(alert.getSeverity())
                .isEqualTo(AlertSeverity.HIGH);

        assertThat(alert.getMessage())
                .contains("RC-2026-015")
                .contains("Battery control unit inspection");

        assertThat(alert.getMeasuredValue())
                .isEqualTo("RC-2026-015");
    }

    @Test
    void shouldNotGenerateAlertForCompletedRecall() {

        VehicleAnalysisRequest request =
                createHealthyRequest();

        RecallCampaignResponse recall =
                new RecallCampaignResponse(
                        "RC-2026-015",
                        "Battery control unit inspection",
                        RecallSeverity.HIGH,
                        RecallStatus.COMPLETED
                );

        List<Alert> alerts =
                alertGenerator.generate(
                        request,
                        List.of(recall)
                );

        assertThat(alerts).isEmpty();
    }

    // Méthodes utilitaires

    private VehicleAnalysisRequest createRequestWithEngineTemperature(double temperature) {

        return createCompleteRequest(
                25_000,
                new TirePressureData(
                        2.3,
                        2.2,
                        2.4,
                        2.1
                ),
                80,
                temperature,
                60,
                List.of()
        );
    }

    private VehicleAnalysisRequest createCompleteRequest(
            long mileage,
            TirePressureData tirePressure,
            int batteryLevel,
            double engineTemperature,
            int fuelLevel,
            List<String> errorCodes) {

        return new VehicleAnalysisRequest(
                UUID.randomUUID(),
                "VF1ABC12345678901",
                Instant.now(),
                mileage,
                tirePressure,
                batteryLevel,
                engineTemperature,
                fuelLevel,
                errorCodes
        );
    }

    private VehicleAnalysisRequest createHealthyRequest() {

        return createCompleteRequest(
                25_000,
                new TirePressureData(
                        2.3,
                        2.2,
                        2.4,
                        2.1
                ),
                80,
                90.0,
                60,
                List.of()
        );
    }
}
