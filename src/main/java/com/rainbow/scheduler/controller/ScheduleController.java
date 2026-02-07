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

        Schedule schedule = schedulingService.generateOptimizedSchedule(orders);
        return ResponseEntity.ok(schedulingService.convertToDTO(schedule));
    }

    @GetMapping("/compare")
    public ResponseEntity<ScheduleResponseDTO> compareSchedules() {
        // For simplicity, we just return the latest optimized schedule with FIFO
        // comparison
        List<Order> orders = orderRepository.findAll();
        Schedule schedule = schedulingService.generateOptimizedSchedule(orders);
        return ResponseEntity.ok(schedulingService.convertToDTO(schedule));
    }
}
