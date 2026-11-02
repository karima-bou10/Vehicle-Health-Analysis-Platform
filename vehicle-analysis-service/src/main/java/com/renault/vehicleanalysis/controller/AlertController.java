package com.renault.vehicleanalysis.controller;

import com.renault.vehicleanalysis.dto.response.VehicleAlertsResponse;
import com.renault.vehicleanalysis.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/{vin}/alerts")
    public ResponseEntity<VehicleAlertsResponse> getAlerts(@PathVariable String vin) {
        return ResponseEntity.ok(alertService.getVehicleAlerts(vin));
    }
}