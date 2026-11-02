package com.renault.vehicleanalysis.repository;

import com.renault.vehicleanalysis.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByVinOrderByCreatedAtDesc(String vin);
}