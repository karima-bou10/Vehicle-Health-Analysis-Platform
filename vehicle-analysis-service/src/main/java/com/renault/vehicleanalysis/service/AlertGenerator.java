package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.external.RecallCampaignResponse;
import com.renault.vehicleanalysis.dto.request.TirePressureData;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.entity.*;
import com.renault.vehicleanalysis.enums.AlertSeverity;
import com.renault.vehicleanalysis.enums.AlertType;
import com.renault.vehicleanalysis.enums.RecallSeverity;
import com.renault.vehicleanalysis.enums.RecallStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AlertGenerator {

    public List<Alert> generate(VehicleAnalysisRequest request, List<RecallCampaignResponse> recalls) {
        List<Alert> alerts = new ArrayList<>();

        generateTirePressureAlerts(
                request.vin(),
                request.tirePressure(),
                alerts
        );

        generateBatteryAlert(
                request.vin(),
                request.batteryLevelPercent(),
                alerts
        );

        generateEngineTemperatureAlert(
                request.vin(),
                request.engineTemperatureCelsius(),
                alerts
        );

        generateFuelAlert(
                request.vin(),
                request.fuelLevelPercent(),
                alerts
        );

        generateErrorCodeAlerts(
                request.vin(),
                request.errorCodes(),
                alerts
        );

        generateRecallAlerts(
                request.vin(),
                recalls,
                alerts
        );
        return alerts;
    }

    private void generateTirePressureAlerts(String vin, TirePressureData tirePressure, List<Alert> alerts) {
        evaluateTirePressure(
                vin,
                "front-left",
                tirePressure.frontLeft(),
                alerts
        );

        evaluateTirePressure(
                vin,
                "front-right",
                tirePressure.frontRight(),
                alerts
        );

        evaluateTirePressure(
                vin,
                "rear-left",
                tirePressure.rearLeft(),
                alerts
        );

        evaluateTirePressure(
                vin,
                "rear-right",
                tirePressure.rearRight(),
                alerts
        );
    }

    private void evaluateTirePressure(String vin, String position, double pressure, List<Alert> alerts) {
        if (pressure < 1.8) {
            alerts.add(createAlert(
                    vin,
                    AlertType.TIRE_PRESSURE,
                    AlertSeverity.HIGH,
                    position
                            + " tire pressure is critically low",
                    pressure + " bar",
                    "minimum 2.0 bar"
            ));
            return;
        }

        if (pressure < 2.0) {
            alerts.add(createAlert(
                    vin,
                    AlertType.TIRE_PRESSURE,
                    AlertSeverity.MEDIUM,
                    position + " tire pressure is low",
                    pressure + " bar",
                    "minimum 2.0 bar"
            ));
            return;
        }

        if (pressure > 2.8) {
            alerts.add(createAlert(
                    vin,
                    AlertType.TIRE_PRESSURE,
                    AlertSeverity.MEDIUM,
                    position + " tire pressure is too high",
                    pressure + " bar",
                    "maximum 2.8 bar"
            ));
        }
    }

    private void generateBatteryAlert(String vin, int batteryLevel, List<Alert> alerts) {
        if (batteryLevel < 20) {
            alerts.add(createAlert(
                    vin,
                    AlertType.LOW_BATTERY,
                    AlertSeverity.HIGH,
                    "Battery level is critically low",
                    batteryLevel + "%",
                    "minimum 20%"
            ));
            return;
        }

        if (batteryLevel < 50) {
            alerts.add(createAlert(
                    vin,
                    AlertType.LOW_BATTERY,
                    AlertSeverity.MEDIUM,
                    "Battery level is low",
                    batteryLevel + "%",
                    "recommended minimum 50%"
            ));
        }
    }

    private void generateEngineTemperatureAlert(String vin, double temperature, List<Alert> alerts) {
        if (temperature > 115) {
            alerts.add(createAlert(
                    vin,
                    AlertType.ENGINE_TEMPERATURE,
                    AlertSeverity.CRITICAL,
                    "Engine temperature is critical",
                    temperature + " °C",
                    "maximum 105 °C"
            ));
            return;
        }

        if (temperature > 105) {
            alerts.add(createAlert(
                    vin,
                    AlertType.ENGINE_TEMPERATURE,
                    AlertSeverity.HIGH,
                    "Engine temperature is too high",
                    temperature + " °C",
                    "maximum 105 °C"
            ));
        }
    }

    private void generateFuelAlert(String vin, int fuelLevel, List<Alert> alerts) {
        if (fuelLevel < 10) {
            alerts.add(createAlert(
                    vin,
                    AlertType.LOW_FUEL,
                    AlertSeverity.MEDIUM,
                    "Fuel level is critically low",
                    fuelLevel + "%",
                    "recommended minimum 20%"
            ));
            return;
        }

        if (fuelLevel < 20) {
            alerts.add(createAlert(
                    vin,
                    AlertType.LOW_FUEL,
                    AlertSeverity.LOW,
                    "Fuel level is low",
                    fuelLevel + "%",
                    "recommended minimum 20%"
            ));
        }
    }

    private void generateErrorCodeAlerts(String vin, List<String> errorCodes, List<Alert> alerts) {
        if (errorCodes == null || errorCodes.isEmpty()) {
            return;
        }

        errorCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .forEach(code ->
                        alerts.add(createAlert(
                                vin,
                                AlertType.ERROR_CODE,
                                AlertSeverity.HIGH,
                                "Diagnostic error code detected: " + code,
                                code,
                                null
                        ))
                );
    }

    private void generateRecallAlerts(
            String vin,
            List<RecallCampaignResponse> recalls,
            List<Alert> alerts) {

        if (recalls == null || recalls.isEmpty()) {
            return;
        }

        recalls.stream()
                .filter(recall ->
                        recall.status() == RecallStatus.OPEN
                )
                .forEach(recall ->
                        alerts.add(createAlert(
                                vin,
                                AlertType.ACTIVE_RECALL,
                                mapRecallSeverity(recall.severity()),
                                "Active recall "
                                        + recall.campaignId()
                                        + ": "
                                        + recall.description(),
                                recall.campaignId(),
                                "Manufacturer intervention required"
                        ))
                );
    }

    private Alert createAlert(
            String vin,
            AlertType type,
            AlertSeverity severity,
            String message,
            String measuredValue,
            String thresholdValue) {

        Alert alert = new Alert();

        alert.setVin(vin);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setMeasuredValue(measuredValue);
        alert.setThresholdValue(thresholdValue);

        return alert;
    }

    private AlertSeverity mapRecallSeverity(
            RecallSeverity severity) {

        return switch (severity) {
            case LOW -> AlertSeverity.LOW;
            case MEDIUM -> AlertSeverity.MEDIUM;
            case HIGH -> AlertSeverity.HIGH;
            case CRITICAL -> AlertSeverity.CRITICAL;
        };
    }
}