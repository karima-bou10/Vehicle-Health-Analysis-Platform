package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.external.RecallCampaignResponse;
import com.renault.vehicleanalysis.dto.request.TirePressureData;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.enums.HealthStatus;
import com.renault.vehicleanalysis.enums.PenaltyCategory;
import com.renault.vehicleanalysis.enums.RecallSeverity;
import com.renault.vehicleanalysis.enums.RecallStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthScoreCalculatorTest {

    private HealthScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new HealthScoreCalculator();
    }

    @Test
    void shouldKeepScoreAt100WhenMileageIsBelow50000() {
        VehicleAnalysisRequest request =
                createRequest(
                        25_000,
                        2.3,
                        2.2,
                        2.4,
                        2.1
                );

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(100);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .doesNotContain(PenaltyCategory.MILEAGE);
    }

    @Test
    void shouldSubtract10PointsWhenMileageIs125000() {
        VehicleAnalysisRequest request =
                createRequest(
                        125_000,
                        2.3,
                        2.2,
                        2.4,
                        2.1
                );

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(90);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.MILEAGE);
                    assertThat(penalty.points()).isEqualTo(10);
                });
    }

    @Test
    void shouldSubtract15PointsWhenMileageIs150000() {
        VehicleAnalysisRequest request =
                createRequest(
                        150_000,
                        2.3,
                        2.2,
                        2.4,
                        2.1
                );

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(85);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.MILEAGE);
                    assertThat(penalty.points()).isEqualTo(15);
                });
    }

    @Test
    void shouldNotApplyPenaltyWhenAllTirePressuresAreNormal() {

        VehicleAnalysisRequest request =
                createRequest(
                        25_000,
                        2.3,
                        2.2,
                        2.4,
                        2.1
                );
        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(100);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .doesNotContain(PenaltyCategory.TIRE_PRESSURE);
    }

    @Test
    void shouldSubtract3PointsWhenOneTirePressureIsLow() {

        VehicleAnalysisRequest request =
                createRequest(
                        25_000,
                        1.9,
                        2.2,
                        2.4,
                        2.1
                );
        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(97);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.TIRE_PRESSURE);

                    assertThat(penalty.points())
                            .isEqualTo(3);

                    assertThat(penalty.reason())
                            .contains("front-left");
                });
    }

    @Test
    void shouldSubtract8PointsWhenOneTirePressureIsCritical() {

        VehicleAnalysisRequest request =
                createRequest(
                        25_000,
                        2.3,
                        2.2,
                        2.4,
                        1.7
                );
        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(92);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.TIRE_PRESSURE);

                    assertThat(penalty.points())
                            .isEqualTo(8);

                    assertThat(penalty.reason())
                            .contains("rear-right");
                });
    }

    @Test
    void shouldSubtract5PointsWhenOneTirePressureIsTooHigh() {

        VehicleAnalysisRequest request =
                createRequest(
                        25_000,
                        3.0,
                        2.2,
                        2.4,
                        2.1
                );
        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(95);
    }

    @Test
    void shouldLimitTirePressurePenaltyTo20Points() {

        VehicleAnalysisRequest request =
                createRequest(
                        25_000,
                        1.5,
                        1.5,
                        1.5,
                        1.5
                );
        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(80);

        assertThat(result.penalties())
                .filteredOn(
                        penalty -> penalty.category()
                                == PenaltyCategory.TIRE_PRESSURE
                )
                .singleElement()
                .extracting(PenaltyResult::points)
                .isEqualTo(20);
    }

    @Test
    void shouldAccumulateMileageAndTirePressurePenalties() {

        VehicleAnalysisRequest request =
                createRequest(
                        125_000,
                        2.3,
                        2.2,
                        2.4,
                        1.7
                );

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(82);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .containsExactlyInAnyOrder(
                        PenaltyCategory.MILEAGE,
                        PenaltyCategory.TIRE_PRESSURE
                );
    }

    @Test
    void shouldNotApplyPenaltyWhenEngineTemperatureIsNormal() {

        VehicleAnalysisRequest request =
                createRequestWithEngineTemperature(105.0);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(100);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .doesNotContain(
                        PenaltyCategory.ENGINE_TEMPERATURE
                );
    }

    @Test
    void shouldSubtract10PointsWhenEngineTemperatureIsHigh() {

        VehicleAnalysisRequest request =
                createRequestWithEngineTemperature(108.0);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(90);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(
                                    PenaltyCategory.ENGINE_TEMPERATURE
                            );
                    assertThat(penalty.points()).isEqualTo(10);
                });
    }

    @Test
    void shouldAddEngineTemperaturePenaltyWhenTemperatureIsCritical() {

        VehicleAnalysisRequest request =
                createRequestWithEngineTemperature(120.0);

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(75);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(
                                    PenaltyCategory.ENGINE_TEMPERATURE
                            );

                    assertThat(penalty.points()).isEqualTo(25);

                    assertThat(penalty.reason())
                            .contains("120.0");
                });
    }

    @Test
    void shouldNotApplyPenaltyWhenFuelLevelIsNormal() {

        VehicleAnalysisRequest request =
                createRequestWithFuelLevel(60);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(100);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .doesNotContain(PenaltyCategory.FUEL);
    }

    @Test
    void shouldSubtract3PointsWhenFuelLevelIsLow() {

        VehicleAnalysisRequest request =
                createRequestWithFuelLevel(18);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(97);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.FUEL);
                    assertThat(penalty.points()).isEqualTo(3);
                });
    }

    @Test
    void shouldSubtract8PointsWhenFuelLevelIsCritical() {

        VehicleAnalysisRequest request =
                createRequestWithFuelLevel(5);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(92);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.FUEL);
                    assertThat(penalty.points()).isEqualTo(8);
                });
    }

    @Test
    void shouldSubtract5PointsWhenBatteryLevelIsLow() {

        VehicleAnalysisRequest request =
                createRequestWithBatteryLevel(35);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(95);
    }

    @Test
    void shouldSubtract15PointsWhenBatteryLevelIsCritical() {

        VehicleAnalysisRequest request =
                createRequestWithBatteryLevel(10);

        HealthScoreResult result =
                calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(85);
    }

    @Test
    void shouldNotApplyPenaltyWhenThereAreNoErrorCodes() {

        VehicleAnalysisRequest request =
                createRequestWithErrorCodes(List.of());

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(100);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .doesNotContain(PenaltyCategory.ERROR_CODE);
    }

    @Test
    void shouldSubtract10PointsForOneErrorCode() {

        VehicleAnalysisRequest request =
                createRequestWithErrorCodes(
                        List.of("P0217")
                );

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(90);
    }

    @Test
    void shouldNotCountDuplicateErrorCodesTwice() {

        VehicleAnalysisRequest request =
                createRequestWithErrorCodes(
                        List.of(
                                "P0217",
                                "p0217",
                                " P0217 "
                        )
                );

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(90);
    }

    @Test
    void shouldAccumulateAllImplementedPenalties() {

        VehicleAnalysisRequest request =
                createCompleteRequest(
                        125_000,
                        new TirePressureData(
                                2.3,
                                2.2,
                                2.4,
                                1.7
                        ),
                        35,
                        108.0,
                        18,
                        List.of("P0217", "P0562")
                );

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isEqualTo(44);

        assertThat(result.status())
                .isEqualTo(HealthStatus.DEGRADED);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .containsExactlyInAnyOrder(
                        PenaltyCategory.MILEAGE,
                        PenaltyCategory.TIRE_PRESSURE,
                        PenaltyCategory.BATTERY,
                        PenaltyCategory.ENGINE_TEMPERATURE,
                        PenaltyCategory.FUEL,
                        PenaltyCategory.ERROR_CODE
                );
    }

    @Test
    void shouldNeverReturnANegativeScore() {

        VehicleAnalysisRequest request =
                createCompleteRequest(
                        200_000,
                        new TirePressureData(
                                1.0,
                                1.0,
                                1.0,
                                1.0
                        ),
                        5,
                        130.0,
                        2,
                        List.of(
                                "P0217",
                                "P0562",
                                "P0300",
                                "C0035"
                        )
                );

        HealthScoreResult result = calculator.calculate(request, List.of());

        assertThat(result.score()).isZero();
        assertThat(result.status())
                .isEqualTo(HealthStatus.CRITICAL);
    }

    @Test
    void shouldSubtract15PointsForOpenHighRecall() {

        VehicleAnalysisRequest request = createHealthyRequest();

        RecallCampaignResponse recall =
                new RecallCampaignResponse(
                        "RC-2026-015",
                        "Battery control unit inspection",
                        RecallSeverity.HIGH,
                        RecallStatus.OPEN
                );

        HealthScoreResult result =
                calculator.calculate(
                        request,
                        List.of(recall)
                );

        assertThat(result.score()).isEqualTo(85);

        assertThat(result.penalties())
                .anySatisfy(penalty -> {
                    assertThat(penalty.category())
                            .isEqualTo(PenaltyCategory.RECALL);
                    assertThat(penalty.points()).isEqualTo(15);
                    assertThat(penalty.reason())
                            .contains("RC-2026-015");
                });
    }

    @Test
    void shouldIgnoreCompletedRecall() {

        VehicleAnalysisRequest request = createHealthyRequest();

        RecallCampaignResponse recall =
                new RecallCampaignResponse(
                        "RC-2026-015",
                        "Battery control unit inspection",
                        RecallSeverity.HIGH,
                        RecallStatus.COMPLETED
                );

        HealthScoreResult result =
                calculator.calculate(
                        request,
                        List.of(recall)
                );

        assertThat(result.score()).isEqualTo(100);

        assertThat(result.penalties())
                .extracting(PenaltyResult::category)
                .doesNotContain(PenaltyCategory.RECALL);
    }

    // Méthodes utilitaires

    private VehicleAnalysisRequest createRequest(
            long mileage,
            double frontLeft,
            double frontRight,
            double rearLeft,
            double rearRight) {

        return new VehicleAnalysisRequest(
                UUID.randomUUID(),
                "VF1ABC12345678901",
                Instant.now(),
                mileage,
                new TirePressureData(
                        frontLeft,
                        frontRight,
                        rearLeft,
                        rearRight
                ),
                80,
                90.0,
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

    private VehicleAnalysisRequest createRequestWithFuelLevel(int fuelLevel) {

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
                fuelLevel,
                List.of()
        );
    }

    private VehicleAnalysisRequest createRequestWithErrorCodes(List<String> errorCodes) {

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
                errorCodes
        );
    }

    private VehicleAnalysisRequest createRequestWithBatteryLevel(int batteryLevel) {

        return createCompleteRequest(
                25_000,
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
