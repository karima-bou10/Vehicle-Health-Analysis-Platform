package com.renault.vehicleanalysis.repository;

import com.renault.vehicleanalysis.entity.HealthReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HealthReportRepository extends JpaRepository<HealthReport, UUID> {

    Optional<HealthReport> findByTelemetryId(UUID telemetryId);

    @EntityGraph(attributePaths = "penalties")
    Optional<HealthReport> findFirstByVinOrderByAnalyzedAtDesc(String vin);
}
