package com.renault.vehicleanalysis.entity;

import com.renault.vehicleanalysis.enums.HealthStatus;
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
@Table(
        name = "health_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_health_report_telemetry",
                        columnNames = "telemetry_id"
                )
        }
)
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "telemetry_id", nullable = false)
    private UUID telemetryId;

    @Column(nullable = false, length = 17)
    private String vin;

    @Column(nullable = false)
    private Integer healthScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthStatus status;

    @Column(nullable = false)
    private Instant telemetryTimestamp;

    @Column(nullable = false)
    private Instant analyzedAt;

    @OneToMany(
            mappedBy = "healthReport",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ScorePenalty> penalties = new ArrayList<>();

    @OneToMany(
            mappedBy = "healthReport",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Alert> alerts = new ArrayList<>();

    public void addPenalty(ScorePenalty penalty) {
        penalties.add(penalty);
        penalty.setHealthReport(this);
    }

    public void addAlert(Alert alert) {
        alerts.add(alert);
        alert.setHealthReport(this);
    }
}
