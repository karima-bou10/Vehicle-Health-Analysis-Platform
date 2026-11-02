package com.renault.telemetry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "telemetry")
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 17)
    private String vin;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Long mileageKm;

    @Column(nullable = false)
    private Double frontLeftTirePressure;

    @Column(nullable = false)
    private Double frontRightTirePressure;

    @Column(nullable = false)
    private Double rearLeftTirePressure;

    @Column(nullable = false)
    private Double rearRightTirePressure;

    @Column(nullable = false)
    private Integer batteryLevelPercent;

    @Column(nullable = false)
    private Double engineTemperatureCelsius;

    @Column(nullable = false)
    private Integer fuelLevelPercent;

    @ElementCollection
    @CollectionTable(
            name = "telemetry_error_codes",
            joinColumns = @JoinColumn(name = "telemetry_id")
    )
    @Column(name = "error_code")
    private List<String> errorCodes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TelemetryStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void beforeInsert() {
        createdAt = Instant.now();
    }
}
