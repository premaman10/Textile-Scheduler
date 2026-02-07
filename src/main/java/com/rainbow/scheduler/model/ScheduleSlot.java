package com.rainbow.scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private int cleaningBeforeMinutes;

    @Enumerated(EnumType.STRING)
    private ColorFamily colorFamily;
}
