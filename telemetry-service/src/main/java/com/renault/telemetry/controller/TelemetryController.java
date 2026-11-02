package com.renault.telemetry.controller;

import com.renault.telemetry.dto.request.CreateTelemetryRequest;
import com.renault.telemetry.dto.response.TelemetryAcceptedResponse;
import com.renault.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(
            TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public ResponseEntity<TelemetryAcceptedResponse> receive(
            @Valid @RequestBody CreateTelemetryRequest request) {

        TelemetryAcceptedResponse response =
                telemetryService.receive(request);

        return ResponseEntity.accepted().body(response);
    }
}
