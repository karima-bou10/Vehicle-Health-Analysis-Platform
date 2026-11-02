package com.renault.vehicleanalysis.entity;

import com.renault.vehicleanalysis.enums.AlertSeverity;
import com.renault.vehicleanalysis.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "idx_alert_vin", columnList = "vin"),
                @Index(name = "idx_alert_created_at", columnList = "created_at")
        }
)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 17)
    private String vin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 100)
    private String measuredValue;

    @Column(length = 100)
    private String thresholdValue;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_report_id",
            nullable = false
    )
    private HealthReport healthReport;

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}