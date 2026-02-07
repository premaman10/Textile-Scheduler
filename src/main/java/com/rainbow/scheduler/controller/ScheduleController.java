package com.rainbow.scheduler.controller;

import com.rainbow.scheduler.dto.ScheduleResponseDTO;
import com.rainbow.scheduler.model.Order;
import com.rainbow.scheduler.model.Schedule;
import com.rainbow.scheduler.repository.OrderRepository;
import com.rainbow.scheduler.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final SchedulingService schedulingService;
    private final OrderRepository orderRepository;

    @PostMapping("/generate")
    public ResponseEntity<ScheduleResponseDTO> generateSchedule() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Group by Simulation Run ID (null key for manual orders)
        java.util.Map<Long, List<Order>> groups = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        o -> o.getSimulationRunId() == null ? -1L : o.getSimulationRunId()));

        if (groups.size() <= 1) {
            // Standard single-batch behavior
            Schedule schedule = schedulingService.generateOptimizedSchedule(orders);
            return ResponseEntity.ok(schedulingService.convertToDTO(schedule));
        }

        // Multi-batch/Simulation behavior (1000 orders case)
        List<ScheduleResponseDTO.SlotDTO> allSlots = new java.util.ArrayList<>();
        int totalCleaning = 0;
        int totalFifo = 0;
        Schedule lastSchedule = null;

        for (List<Order> batch : groups.values()) {
            Schedule batchSchedule = schedulingService.generateOptimizedSchedule(batch);
            // This triggers the DB update for this specific run
            schedulingService.convertToDTO(batchSchedule);

            // Aggregate for display
            totalCleaning += batchSchedule.getTotalCleaningTimeMinutes();
            totalFifo += batchSchedule.getFifoCleaningTimeMinutes();

            ScheduleResponseDTO batchDTO = schedulingService.convertToDTO(batchSchedule);
            allSlots.addAll(batchDTO.getSchedule());
            lastSchedule = batchSchedule;
        }

        // Create a synthetic response for the frontend dashboard
        int timeSaved = totalFifo - totalCleaning;

        return ResponseEntity.ok(ScheduleResponseDTO.builder()
                .optimizedCleaningTimeMinutes(totalCleaning)
                .fifoCleaningTimeMinutes(totalFifo)
                .timeSavedMinutes(Math.max(0, timeSaved))
                .deadlineCompliance("N/A (Multi-Batch)") // Complex to aggregate
                .machineEfficiency("100%") // Placeholder for combined view
                .schedule(allSlots)
                .build());
    }
}
