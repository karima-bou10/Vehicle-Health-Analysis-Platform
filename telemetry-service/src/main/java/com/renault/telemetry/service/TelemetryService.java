package com.renault.telemetry.service;

import com.renault.telemetry.dto.request.CreateTelemetryRequest;
import com.renault.telemetry.dto.response.TelemetryAcceptedResponse;
import com.renault.telemetry.entity.Telemetry;
import com.renault.telemetry.entity.TelemetryStatus;
import com.renault.telemetry.repository.TelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final AnalysisDispatcher analysisDispatcher;

    public TelemetryService(
            TelemetryRepository telemetryRepository,
            AnalysisDispatcher analysisDispatcher) {
        this.telemetryRepository = telemetryRepository;
        this.analysisDispatcher = analysisDispatcher;
    }

    //@Transactional
    public TelemetryAcceptedResponse receive(
            CreateTelemetryRequest request) {

        Telemetry telemetry = new Telemetry();

        telemetry.setVin(request.vin());
        telemetry.setTimestamp(request.timestamp());
        telemetry.setMileageKm(request.mileageKm());
        telemetry.setFrontLeftTirePressure(
                request.tirePressure().frontLeft()
        );
        telemetry.setFrontRightTirePressure(
                request.tirePressure().frontRight()
        );
        telemetry.setRearLeftTirePressure(
                request.tirePressure().rearLeft()
        );
        telemetry.setRearRightTirePressure(
                request.tirePressure().rearRight()
        );
        telemetry.setBatteryLevelPercent(
                request.batteryLevelPercent()
        );
        telemetry.setEngineTemperatureCelsius(
                request.engineTemperatureCelsius()
        );
        telemetry.setFuelLevelPercent(
                request.fuelLevelPercent()
        );
        telemetry.setErrorCodes(
                request.errorCodes() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(request.errorCodes())
        );
        telemetry.setStatus(TelemetryStatus.RECEIVED);

        Telemetry saved = telemetryRepository.save(telemetry);

        analysisDispatcher.dispatch(saved.getId());

        return new TelemetryAcceptedResponse(
                saved.getId(),
                saved.getVin(),
                TelemetryStatus.RECEIVED,
                "Telemetry accepted for analysis"
        );
    }
}
