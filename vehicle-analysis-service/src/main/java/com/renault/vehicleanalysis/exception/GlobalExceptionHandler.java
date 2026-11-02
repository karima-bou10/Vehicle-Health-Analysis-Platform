package com.renault.vehicleanalysis.exception;

import com.renault.vehicleanalysis.dto.response.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HealthReportNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            HealthReportNotFoundException exception) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "HEALTH_REPORT_NOT_FOUND",
                        exception.getMessage(),
                        List.of()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception) {

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                List.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}