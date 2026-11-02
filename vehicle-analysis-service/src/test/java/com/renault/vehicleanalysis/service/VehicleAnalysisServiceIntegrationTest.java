package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.request.TirePressureData;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.dto.response.VehicleAnalysisResponse;
import com.renault.vehicleanalysis.entity.HealthReport;
import com.renault.vehicleanalysis.enums.PenaltyCategory;
import com.renault.vehicleanalysis.entity.ScorePenalty;
import com.renault.vehicleanalysis.repository.HealthReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Test d’intégration
@Transactional // fait un rollback à la fin du test
class VehicleAnalysisServiceIntegrationTest {

    @Autowired
    private VehicleAnalysisService vehicleAnalysisService;

    @Autowired
    private HealthReportRepository healthReportRepository;

    @Test
    void shouldSaveHealthReportWithPenalties() {

        VehicleAnalysisRequest request =
                createCompleteRequest(125_000,
                        new TirePressureData(
                                2.3,
                                2.2,
                                2.4,
                                1.7),
                        35,
                        108.0,
                        18,
                        List.of("P0217", "P0562"));

        VehicleAnalysisResponse response =
                vehicleAnalysisService.analyze(request);

        HealthReport report = healthReportRepository
                .findById(response.analysisId())
                .orElseThrow();

        assertThat(report.getHealthScore()).isEqualTo(44);

        assertThat(report.getPenalties())
                .hasSize(6)
                .extracting(ScorePenalty::getCategory)
                .containsExactlyInAnyOrder(
                        PenaltyCategory.MILEAGE,
                        PenaltyCategory.TIRE_PRESSURE,
                        PenaltyCategory.BATTERY,
                        PenaltyCategory.ENGINE_TEMPERATURE,
                        PenaltyCategory.FUEL,
                        PenaltyCategory.ERROR_CODE
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
}