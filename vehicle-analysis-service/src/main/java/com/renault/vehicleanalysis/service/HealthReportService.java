package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.response.HealthReportResponse;
import com.renault.vehicleanalysis.dto.response.PenaltyResponse;
import com.renault.vehicleanalysis.entity.HealthReport;
import com.renault.vehicleanalysis.exception.HealthReportNotFoundException;
import com.renault.vehicleanalysis.repository.HealthReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HealthReportService {

    private final HealthReportRepository healthReportRepository;

    public HealthReportService(
            HealthReportRepository healthReportRepository) {
        this.healthReportRepository = healthReportRepository;
    }

    @Transactional(readOnly = true)
    public HealthReportResponse getLatestReport(String vin) {

        HealthReport report = healthReportRepository
                .findFirstByVinOrderByAnalyzedAtDesc(vin)
                .orElseThrow(
                        () -> new HealthReportNotFoundException(vin)
                );

        List<PenaltyResponse> penalties = report.getPenalties()
                .stream()
                .map(penalty -> new PenaltyResponse(
                        penalty.getCategory().name(),
                        penalty.getPoints(),
                        penalty.getReason()
                ))
                .toList();

        return new HealthReportResponse(
                report.getId(),
                report.getTelemetryId(),
                report.getVin(),
                report.getHealthScore(),
                report.getStatus().name(),
                report.getTelemetryTimestamp(),
                report.getAnalyzedAt(),
                penalties
        );
    }
}
