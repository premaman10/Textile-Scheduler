package com.rainbow.scheduler.controller;

import com.rainbow.scheduler.model.Order;
import com.rainbow.scheduler.model.SimulationRun;
import com.rainbow.scheduler.repository.OrderRepository;
import com.rainbow.scheduler.repository.SimulationRunRepository;
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

    @GetMapping
    public List<SimulationRun> getRecentSimulations() {
        return simulationRunRepository.findTop10ByOrderByTimestampDesc();
    }

    @GetMapping("/{id}/orders")
    public List<Order> getSimulationOrders(@PathVariable Long id) {
        return orderRepository.findBySimulationRunId(id);
    }
}
