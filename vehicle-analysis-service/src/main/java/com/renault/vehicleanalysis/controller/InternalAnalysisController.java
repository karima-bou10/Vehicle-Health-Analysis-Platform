package com.renault.vehicleanalysis.controller;

import com.renault.vehicleanalysis.dto.request.VehicleAnalysisRequest;
import com.renault.vehicleanalysis.dto.response.VehicleAnalysisResponse;
import com.renault.vehicleanalysis.service.VehicleAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/api/v1/analyses")
public class InternalAnalysisController {

    private final VehicleAnalysisService vehicleAnalysisService;

    public InternalAnalysisController(VehicleAnalysisService vehicleAnalysisService) {
        this.vehicleAnalysisService = vehicleAnalysisService;
    }

    @PostMapping
    public ResponseEntity<VehicleAnalysisResponse> analyze(
            @Valid @RequestBody VehicleAnalysisRequest request) {

        VehicleAnalysisResponse response = vehicleAnalysisService.analyze(request);
        return ResponseEntity.ok(response);
    }
}
