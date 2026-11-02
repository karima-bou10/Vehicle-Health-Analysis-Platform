package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.external.RecallCampaignResponse;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.enums.HealthStatus;
import com.renault.vehicleanalysis.enums.PenaltyCategory;
import com.renault.vehicleanalysis.dto.request.TirePressureData;
import com.renault.vehicleanalysis.enums.RecallStatus;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HealthScoreCalculator {
    private static final int MAX_TIRE_PRESSURE_PENALTY = 20;
    private static final int ERROR_CODE_PENALTY = 10;
    private static final int MAX_ERROR_CODE_PENALTY = 30;
    private static final int MAX_RECALL_PENALTY = 40;

    public HealthScoreResult calculate(
            VehicleAnalysisRequest request,
            List<RecallCampaignResponse> recalls) {

        List<PenaltyResult> penalties = new ArrayList<>();

        calculateMileagePenalty(request, penalties);
        calculateTirePressurePenalty(request, penalties);
        calculateBatteryPenalty(request, penalties);
        calculateEngineTemperaturePenalty(request, penalties);
        calculateFuelPenalty(request, penalties);
        calculateErrorCodePenalty(request, penalties);
        calculateRecallPenalty(recalls, penalties);

        int totalPenalty = penalties.stream()
                .mapToInt(PenaltyResult::points)
                .sum();

        int score = Math.max(0, 100 - totalPenalty);

        return new HealthScoreResult(
                score,
                determineStatus(score),
                penalties
        );
    }

    private HealthStatus determineStatus(int score) {
        if (score >= 80) {
            return HealthStatus.HEALTHY;
        }

        if (score >= 60) {
            return HealthStatus.WARNING;
        }

        if (score >= 40) {
            return HealthStatus.DEGRADED;
        }

        return HealthStatus.CRITICAL;
    }

    private void calculateMileagePenalty(VehicleAnalysisRequest request, List<PenaltyResult> penalties) {

        long mileage = request.mileageKm();

        if (mileage >= 150_000) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.MILEAGE,
                    15,
                    "Mileage is at least 150000 km"
            ));
        } else if (mileage >= 100_000) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.MILEAGE,
                    10,
                    "Mileage is between 100000 and 149999 km"
            ));
        } else if (mileage >= 50_000) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.MILEAGE,
                    5,
                    "Mileage is between 50000 and 99999 km"
            ));
        }
    }

    private void calculateTirePressurePenalty(VehicleAnalysisRequest request, List<PenaltyResult> penalties) {

        TirePressureData tirePressure = request.tirePressure();

        int totalPenalty = 0;
        List<String> reasons = new ArrayList<>();

        totalPenalty += evaluateTire(
                "front-left",
                tirePressure.frontLeft(),
                reasons
        );

        totalPenalty += evaluateTire(
                "front-right",
                tirePressure.frontRight(),
                reasons
        );

        totalPenalty += evaluateTire(
                "rear-left",
                tirePressure.rearLeft(),
                reasons
        );

        totalPenalty += evaluateTire(
                "rear-right",
                tirePressure.rearRight(),
                reasons
        );

        int limitedPenalty = Math.min(
                totalPenalty,
                MAX_TIRE_PRESSURE_PENALTY
        );

        if (limitedPenalty > 0) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.TIRE_PRESSURE,
                    limitedPenalty,
                    String.join("; ", reasons)
            ));
        }
    }

    private int evaluateTire(String tirePosition, double pressure, List<String> reasons) {
        if (pressure < 1.8) {
            reasons.add(
                    tirePosition
                            + " tire pressure is critically low: "
                            + pressure
                            + " bar"
            );
            return 8;
        }
        if (pressure < 2.0) {
            reasons.add(
                    tirePosition
                            + " tire pressure is low: "
                            + pressure
                            + " bar"
            );
            return 3;
        }
        if (pressure > 2.8) {
            reasons.add(
                    tirePosition
                            + " tire pressure is too high: "
                            + pressure
                            + " bar"
            );
            return 5;
        }
        return 0;
    }

    private void calculateBatteryPenalty(
            VehicleAnalysisRequest request,
            List<PenaltyResult> penalties) {

        int batteryLevel = request.batteryLevelPercent();

        if (batteryLevel < 20) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.BATTERY,
                    15,
                    "Battery level is critically low: "
                            + batteryLevel
                            + "%"
            ));
        } else if (batteryLevel < 50) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.BATTERY,
                    5,
                    "Battery level is low: "
                            + batteryLevel
                            + "%"
            ));
        }
    }

    private void calculateEngineTemperaturePenalty(VehicleAnalysisRequest request, List<PenaltyResult> penalties) {

        double temperature =
                request.engineTemperatureCelsius();

        if (temperature > 115) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.ENGINE_TEMPERATURE,
                    25,
                    "Engine temperature is critical: "
                            + temperature
                            + " °C"
            ));
        } else if (temperature > 105) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.ENGINE_TEMPERATURE,
                    10,
                    "Engine temperature is high: "
                            + temperature
                            + " °C"
            ));
        }
    }

    private void calculateFuelPenalty(VehicleAnalysisRequest request, List<PenaltyResult> penalties) {

        int fuelLevel = request.fuelLevelPercent();
        if (fuelLevel < 10) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.FUEL,
                    8,
                    "Fuel level is critically low: "
                            + fuelLevel
                            + "%"
            ));
        } else if (fuelLevel < 20) {
            penalties.add(new PenaltyResult(
                    PenaltyCategory.FUEL,
                    3,
                    "Fuel level is low: "
                            + fuelLevel
                            + "%"
            ));
        }
    }

    private void calculateErrorCodePenalty(VehicleAnalysisRequest request, List<PenaltyResult> penalties) {

        if (request.errorCodes() == null
                || request.errorCodes().isEmpty()) {
            return;
        }

        List<String> uniqueCodes = request.errorCodes()
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (uniqueCodes.isEmpty()) {
            return;
        }

        int calculatedPenalty =
                uniqueCodes.size() * ERROR_CODE_PENALTY;

        int limitedPenalty = Math.min(
                calculatedPenalty,
                MAX_ERROR_CODE_PENALTY
        );

        penalties.add(new PenaltyResult(
                PenaltyCategory.ERROR_CODE,
                limitedPenalty,
                "Diagnostic error codes detected: "
                        + String.join(", ", uniqueCodes)
        ));
    }

    private void calculateRecallPenalty(List<RecallCampaignResponse> recalls, List<PenaltyResult> penalties) {
        if (recalls == null || recalls.isEmpty()) {
            return;
        }

        List<RecallCampaignResponse> openRecalls = recalls.stream()
                .filter(recall -> recall.status() == RecallStatus.OPEN)
                .toList();

        if (openRecalls.isEmpty()) {
            return;
        }

        int calculatedPenalty = openRecalls.stream()
                .mapToInt(this::getRecallPenalty)
                .sum();

        int limitedPenalty = Math.min(
                calculatedPenalty,
                MAX_RECALL_PENALTY
        );

        String campaignIds = openRecalls.stream()
                .map(RecallCampaignResponse::campaignId)
                .collect(Collectors.joining(", "));

        penalties.add(new PenaltyResult(
                PenaltyCategory.RECALL,
                limitedPenalty,
                "Active recall campaigns detected: " + campaignIds
        ));
    }

    private int getRecallPenalty(RecallCampaignResponse recall) {

        return switch (recall.severity()) {
            case LOW -> 3;
            case MEDIUM -> 7;
            case HIGH -> 15;
            case CRITICAL -> 25;
        };
    }
}
