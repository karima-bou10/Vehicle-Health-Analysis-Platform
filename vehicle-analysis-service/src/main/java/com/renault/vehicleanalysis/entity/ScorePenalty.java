package com.renault.vehicleanalysis.entity;

import com.renault.vehicleanalysis.enums.PenaltyCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "score_penalties")
public class ScorePenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PenaltyCategory category;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "health_report_id",
            nullable = false
    )
    private HealthReport healthReport;
}
