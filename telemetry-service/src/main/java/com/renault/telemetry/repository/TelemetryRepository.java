package com.renault.telemetry.repository;

import com.renault.telemetry.entity.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, UUID> {
}