package com.rainbow.scheduler.controller;

import com.rainbow.scheduler.model.*;
import com.rainbow.scheduler.repository.OrderRepository;
import com.rainbow.scheduler.repository.SimulationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoDataController {

    private final OrderRepository orderRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final Random random = new Random();

    @PostMapping("/populate")
    public String populateData() {
        return generateOrders(100, false);
    }

    @PostMapping("/simulate")
    public String simulatePeakHours() {
        // Create a new Simulation Run record
        long runNumber = simulationRunRepository.count() + 1;
        SimulationRun run = SimulationRun.builder()
                .name("Test Case " + runNumber)
                .timestamp(LocalDateTime.now())
                .orderCount(100)
                .build();
        run = simulationRunRepository.save(run);

        return generateOrders(100, true, run.getId());
    }

    private String generateOrders(int count, boolean isSimulation) {
        return generateOrders(count, isSimulation, null);
    }

    private String generateOrders(int count, boolean isSimulation, Long runId) {
        List<Order> orders = new ArrayList<>();
        ColorFamily[] families = ColorFamily.values();

        for (int i = 0; i < count; i++) {
            OrderType type;
            int deadline;

            if (isSimulation) {
                double rand = random.nextDouble();
                if (rand < 0.3) {
                    type = OrderType.RUSH;
                    deadline = 8 + random.nextInt(5); // 8-12h
                } else if (rand < 0.8) {
                    type = OrderType.STANDARD;
                    deadline = 24 + random.nextInt(25); // 24-48h
                } else {
                    type = OrderType.BULK;
                    deadline = 48 + random.nextInt(25); // 48-72h
                }
            } else {
                type = OrderType.values()[random.nextInt(OrderType.values().length)];
                deadline = 8 + random.nextInt(64);
            }

            orders.add(Order.builder()
                    .colorName("Order-" + (i + 1))
                    .colorFamily(families[random.nextInt(families.length)])
                    .quantityMeters(100 + random.nextInt(901))
                    .orderType(type)
                    .deadlineHours(deadline)
                    .createdAt(LocalDateTime.now())
                    .status(com.rainbow.scheduler.model.OrderStatus.PENDING)
                    .simulationRunId(runId)
                    .build());
        }

        orderRepository.saveAll(orders);
        return "Added " + count + (isSimulation ? " simulation" : " demo") + " orders.";
    }
}
