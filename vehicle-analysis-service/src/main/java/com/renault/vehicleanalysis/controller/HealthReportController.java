package com.renault.vehicleanalysis.controller;

import com.renault.vehicleanalysis.dto.response.HealthReportResponse;
import com.renault.vehicleanalysis.service.HealthReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
public class HealthReportController {

    private final HealthReportService healthReportService;

    public HealthReportController(HealthReportService healthReportService) {
        this.healthReportService = healthReportService;
    }

    @GetMapping("/{vin}/health-report")
    public ResponseEntity<HealthReportResponse> getHealthReport(@PathVariable String vin) {
        HealthReportResponse response = healthReportService.getLatestReport(vin);
        return ResponseEntity.ok(response);
    }
}
