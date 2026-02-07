package com.rainbow.scheduler.controller;

import com.rainbow.scheduler.model.Order;
import com.rainbow.scheduler.model.SimulationRun;
import com.rainbow.scheduler.repository.OrderRepository;
import com.rainbow.scheduler.repository.SimulationRunRepository;
import com.rainbow.scheduler.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationRunRepository simulationRunRepository;
    private final OrderRepository orderRepository;
    private final SimulationService simulationService;

    @PostMapping("/run")
    public ResponseEntity<Void> runSimulation() {
        simulationService.runFullSimulation();
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<SimulationRun> getRecentSimulations() {
        return simulationRunRepository.findTop10ByOrderByTimestampDesc();
    }

    @GetMapping("/{id}/orders")
    public List<Order> getSimulationOrders(@PathVariable Long id) {
        return orderRepository.findBySimulationRunId(id);
    }
}
