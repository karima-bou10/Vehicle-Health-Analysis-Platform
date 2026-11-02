package com.renault.vehicleanalysis.service;

import com.renault.vehicleanalysis.dto.response.AlertResponse;
import com.renault.vehicleanalysis.dto.response.VehicleAlertsResponse;
import com.renault.vehicleanalysis.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public VehicleAlertsResponse getVehicleAlerts(String vin) {

        List<AlertResponse> alertResponses =
                alertRepository
                        .findByVinOrderByCreatedAtDesc(vin)
                        .stream()
                        .map(alert -> new AlertResponse(
                                alert.getId(),
                                alert.getType().name(),
                                alert.getSeverity().name(),
                                alert.getMessage(),
                                alert.getMeasuredValue(),
                                alert.getThresholdValue(),
                                alert.getCreatedAt()
                        ))
                        .toList();

        return new VehicleAlertsResponse(
                vin,
                alertResponses.size(),
                alertResponses
        );
    }
}