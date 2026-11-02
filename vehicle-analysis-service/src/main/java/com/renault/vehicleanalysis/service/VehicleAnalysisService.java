package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.client.RecallProviderClient;
import com.renault.vehicleanalysis.dto.external.RecallCampaignResponse;
import com.renault.vehicleanalysis.dto.external.RecallProviderResponse;
import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.dto.response.VehicleAnalysisResponse;
import com.renault.vehicleanalysis.entity.Alert;
import com.renault.vehicleanalysis.entity.HealthReport;
import com.renault.vehicleanalysis.entity.ScorePenalty;
import com.renault.vehicleanalysis.repository.HealthReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class VehicleAnalysisService {

    private final HealthReportRepository healthReportRepository;
    private final HealthScoreCalculator healthScoreCalculator;
    private final AlertGenerator alertGenerator;
    private final RecallProviderClient recallProviderClient;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(VehicleAnalysisService.class);

    public VehicleAnalysisService(
            HealthReportRepository healthReportRepository,
            HealthScoreCalculator healthScoreCalculator,
            AlertGenerator alertGenerator,
            RecallProviderClient recallProviderClient) {

        this.healthReportRepository = healthReportRepository;
        this.healthScoreCalculator = healthScoreCalculator;
        this.alertGenerator = alertGenerator;
        this.recallProviderClient = recallProviderClient;
    }

    public VehicleAnalysisResponse analyze(
            VehicleAnalysisRequest request) {

        return healthReportRepository
                .findByTelemetryId(request.telemetryId())
                .map(this::toResponse)
                .orElseGet(() -> createReport(request));
    }

    private VehicleAnalysisResponse createReport(
            VehicleAnalysisRequest request) {

        List<RecallCampaignResponse> recalls = retrieveRecalls(request.vin());

        HealthScoreResult result = healthScoreCalculator.calculate(request, recalls);

        HealthReport report = new HealthReport();

        report.setTelemetryId(request.telemetryId());
        report.setVin(request.vin());
        report.setHealthScore(result.score());
        report.setStatus(result.status());
        report.setTelemetryTimestamp(request.timestamp());
        report.setAnalyzedAt(Instant.now());

        for (PenaltyResult penaltyResult : result.penalties()) {

            ScorePenalty penalty = new ScorePenalty();
            penalty.setCategory(penaltyResult.category());
            penalty.setPoints(penaltyResult.points());
            penalty.setReason(penaltyResult.reason());

            report.addPenalty(penalty);
        }

        List<Alert> alerts = alertGenerator.generate(request, recalls);
        for (Alert alert : alerts) {
            report.addAlert(alert);
        }

        HealthReport saved = healthReportRepository.save(report);

        return toResponse(saved);
    }

    private VehicleAnalysisResponse toResponse(
            HealthReport report) {

        return new VehicleAnalysisResponse(
                report.getId(),
                report.getVin(),
                report.getHealthScore(),
                report.getStatus().name(),
                report.getAlerts().size()
        );
    }

    private List<RecallCampaignResponse> retrieveRecalls(
            String vin) {

        try {
            RecallProviderResponse response =
                    recallProviderClient.getRecalls(vin);

            if (response == null || response.recalls() == null) {
                return List.of();
            }

            return response.recalls();

        } catch (Exception exception) {
            LOGGER.warn(
                    "Recall provider unavailable for VIN {} , {}",
                    vin,
                    exception.getMessage()
            );

            return List.of();
        }
    }
}