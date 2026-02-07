package com.rainbow.scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String colorName;

    @Enumerated(EnumType.STRING)
    private ColorFamily colorFamily;

    private int quantityMeters;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    private int deadlineHours;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Derived or logic fields
    private double urgencyScore;
    private double productionTimeHours;
    private boolean isCritical;

    private Long simulationRunId; // Link to SimulationRun

    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
}
