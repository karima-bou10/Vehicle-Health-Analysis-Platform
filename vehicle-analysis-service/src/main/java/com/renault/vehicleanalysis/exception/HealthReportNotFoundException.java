package com.renault.vehicleanalysis.exception;

public class HealthReportNotFoundException extends RuntimeException {

    public HealthReportNotFoundException(String vin) {
        super("No health report found for VIN: " + vin);
    }
}
