package com.rainbow.scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Test Case 1"
    private LocalDateTime timestamp;

    private String deadlineCompliance;
    private String machineEfficiency;
    private int totalCleaningTimeMinutes;
    private int timeSavedMinutes;
    private char ecoGrade;
    private int orderCount;
}
