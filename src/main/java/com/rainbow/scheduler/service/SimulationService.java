package com.rainbow.scheduler.service;

import com.rainbow.scheduler.model.*;
import com.rainbow.scheduler.repository.OrderRepository;
import com.rainbow.scheduler.repository.SimulationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final OrderRepository orderRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final SchedulingService schedulingService;
    private final Random random = new Random();

    @Transactional
    public void runFullSimulation() {
        // Run 10 test cases
        for (int i = 1; i <= 10; i++) {
            runSimulationTestCase("Peak Test Case " + i);
        }
    }

    private void runSimulationTestCase(String name) {
        SimulationRun run = SimulationRun.builder()
                .name(name)
                .timestamp(LocalDateTime.now())
                .orderCount(100)
                .ecoGrade("A")
                .build();
        run = simulationRunRepository.save(run);

        List<Order> orders = generateRandomOrders(100, run.getId());
        orderRepository.saveAll(orders);

        // Process this specific simulation run
        // schedulingService.generateOptimizedSchedule(orders); // Removed to allow
        // manual trigger
    }

    private List<Order> generateRandomOrders(int count, Long runId) {
        List<Order> orders = new ArrayList<>();
        ColorFamily[] families = ColorFamily.values();
        OrderType[] types = OrderType.values();
        String[] colors = { "Royal Blue", "Crimson Red", "Forest Green", "Golden Yellow", "Deep Purple", "Sky Blue",
                "Terracotta", "Sage Green", "Navy Blue", "Pastel Pink" };

        for (int i = 0; i < count; i++) {
            orders.add(Order.builder()
                    .colorName(colors[random.nextInt(colors.length)] + " " + (i + 1))
                    .colorFamily(families[random.nextInt(families.length)])
                    .quantityMeters(100 + random.nextInt(901))
                    .orderType(types[random.nextInt(types.length)])
                    .deadlineHours(6 + random.nextInt(91))
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .simulationRunId(runId)
                    .build());
        }
        return orders;
    }
}
