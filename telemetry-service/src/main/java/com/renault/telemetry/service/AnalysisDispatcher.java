package com.renault.telemetry.service;

import com.renault.telemetry.client.VehicleAnalysisClient;
import com.renault.telemetry.dto.client.TirePressureData;
import com.renault.telemetry.dto.client.VehicleAnalysisRequest;
import com.renault.telemetry.dto.response.VehicleAnalysisResponse;
import com.renault.telemetry.entity.Telemetry;
import com.renault.telemetry.entity.TelemetryStatus;
import com.renault.telemetry.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisDispatcher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnalysisDispatcher.class);

    private final VehicleAnalysisClient vehicleAnalysisClient;
    private final TelemetryRepository telemetryRepository;

    public AnalysisDispatcher(
            VehicleAnalysisClient vehicleAnalysisClient,
            TelemetryRepository telemetryRepository) {
        this.vehicleAnalysisClient = vehicleAnalysisClient;
        this.telemetryRepository = telemetryRepository;
    }

    @Async("analysisExecutor")
    @Transactional
    public void dispatch(UUID telemetryId) {

        Telemetry telemetry = telemetryRepository.findById(telemetryId)
                .orElseThrow();

        try {
            telemetry.setStatus(TelemetryStatus.PROCESSING);
            //telemetryRepository.save(telemetry);

            List<String> errorCodes = new ArrayList<>(telemetry.getErrorCodes());

            TirePressureData tirePressure =
                    new TirePressureData(
                            telemetry.getFrontLeftTirePressure(),
                            telemetry.getFrontRightTirePressure(),
                            telemetry.getRearLeftTirePressure(),
                            telemetry.getRearRightTirePressure()
                    );

            VehicleAnalysisRequest request =
                    new VehicleAnalysisRequest(
                            telemetry.getId(),
                            telemetry.getVin(),
                            telemetry.getTimestamp(),
                            telemetry.getMileageKm(),
                            tirePressure,
                            telemetry.getBatteryLevelPercent(),
                            telemetry.getEngineTemperatureCelsius(),
                            telemetry.getFuelLevelPercent(),
                            errorCodes
                    );

            VehicleAnalysisResponse response =
                    vehicleAnalysisClient.requestAnalysis(request);

            telemetry.setStatus(TelemetryStatus.ANALYZED);
            //telemetryRepository.save(telemetry);

            LOGGER.info(
                    "Analysis completed for VIN {} with score {}",
                    telemetry.getVin(),
                    response.healthScore()
            );

        } catch (Exception exception) {
            telemetry.setStatus(TelemetryStatus.FAILED);
            //telemetryRepository.save(telemetry);

            LOGGER.error(
                    "Analysis failed for telemetry {}",
                    telemetryId,
                    exception
            );
        }
    }
}
