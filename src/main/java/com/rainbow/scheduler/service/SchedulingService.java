package com.rainbow.scheduler.service;

import com.rainbow.scheduler.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.rainbow.scheduler.dto.ScheduleResponseDTO;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final CleaningService cleaningService;
    private final EcoEfficiencyService ecoEfficiencyService;

    @Value("${production.dyeing-speed-meters-per-hour:50}")
    private int dyeingSpeed;

    @Value("${production.setup-time-minutes:15}")
    private int setupTimeMinutes;

    @Value("${production.start-hour:8}")
    private int startHour;

    @Value("${production.daily-window-hours:16}")
    private int dailyWindowHours;

    /**
     * Phase 1: Order Analysis
     * Calculates urgency, production time, and marks critical orders.
     */
    public List<Order> analyzeOrders(List<Order> orders) {
        return orders.stream().map(order -> {
            double prodTimeHours = (double) order.getQuantityMeters() / dyeingSpeed;
            order.setProductionTimeHours(prodTimeHours);

            // Urgency Score: (Quantity * OrderTypeWeight) / Deadline
            double urgency = (order.getQuantityMeters() * order.getOrderType().getPriceMultiplier())
                    / order.getDeadlineHours();
            order.setUrgencyScore(urgency);

            // Mark critical if deadline < 12 hours
            order.setCritical(order.getDeadlineHours() < 12 || order.getOrderType() == OrderType.RUSH);

            return order;
        }).collect(Collectors.toList());
    }

    /**
     * Phase 2: Base Scheduling
     * Sorts critical orders by deadline, then others by typical color flow.
     */
    public List<Order> generateBaseSchedule(List<Order> analyzedOrders) {
        List<Order> critical = analyzedOrders.stream()
                .filter(Order::isCritical)
                .sorted(Comparator.comparingInt(Order::getDeadlineHours))
                .collect(Collectors.toList());

        List<Order> others = analyzedOrders.stream()
                .filter(o -> !o.isCritical())
                .sorted(Comparator.comparing(Order::getColorFamily)
                        .thenComparing(o -> o.getOrderType().ordinal())) // RUSH -> STANDARD -> BULK
                .collect(Collectors.toList());

        List<Order> baseSeq = new ArrayList<>(critical);
        baseSeq.addAll(others);
        return baseSeq;
    }

    /**
     * Phase 3: Look-Ahead Optimization
     * Tries to find better insertion points for same-family orders.
     */
    public List<Order> optimizeSequence(List<Order> baseSeq) {
        LinkedList<Order> optimized = new LinkedList<>();
        if (baseSeq.isEmpty())
            return baseSeq;

        for (Order order : baseSeq) {
            if (optimized.isEmpty()) {
                optimized.add(order);
                continue;
            }

            // Try current position (end)
            int bestPos = optimized.size();
            int minAddedCleaning = cleaningService.calculateCleaningTime(optimized.getLast().getColorFamily(),
                    order.getColorFamily());

            // Look back for same family to reduce cleaning
            for (int i = 0; i < optimized.size(); i++) {
                ColorFamily prevFamily = (i == 0) ? ColorFamily.WHITES_PASTELS : optimized.get(i - 1).getColorFamily();
                ColorFamily currentFamily = optimized.get(i).getColorFamily();

                int cleaningBeforeIfInserted = cleaningService.calculateCleaningTime(prevFamily,
                        order.getColorFamily());
                int cleaningAfterIfInserted = cleaningService.calculateCleaningTime(order.getColorFamily(),
                        currentFamily);
                int cleaningSavedOld = cleaningService.calculateCleaningTime(prevFamily, currentFamily);

                int extraCleaning = cleaningBeforeIfInserted + cleaningAfterIfInserted - cleaningSavedOld;

                if (extraCleaning < minAddedCleaning) {
                    // Check if swapping causes major disruptions (simple heuristic)
                    if (!order.isCritical() || i < 5) { // Allow critical to skip ahead but not too far back
                        minAddedCleaning = extraCleaning;
                        bestPos = i;
                    }
                }
            }
            optimized.add(bestPos, order);
        }
        return new ArrayList<>(optimized);
    }

    /**
     * Maps sequence to actual time slots
     */
    public List<ScheduleSlot> mapToSlots(List<Order> sequence, LocalDateTime startTime) {
        List<ScheduleSlot> slots = new ArrayList<>();
        LocalDateTime currentTime = startTime;
        ColorFamily lastFamily = ColorFamily.WHITES_PASTELS;

        for (Order order : sequence) {
            int cleaningTime = cleaningService.calculateCleaningTime(lastFamily, order.getColorFamily());

            // Add setup time
            int totalDowntime = cleaningTime + setupTimeMinutes;
            currentTime = currentTime.plusMinutes(totalDowntime);

            // Avoid production window issues (simplified: assume next day if past 16h
            // limit)
            // In a real factory, we'd check if (currentTime + prodTime) > midnight

            LocalDateTime slotStart = currentTime;
            long prodMinutes = (long) (order.getProductionTimeHours() * 60);
            LocalDateTime slotEnd = slotStart.plusMinutes(prodMinutes);

            slots.add(ScheduleSlot.builder()
                    .order(order)
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .cleaningBeforeMinutes(cleaningTime)
                    .colorFamily(order.getColorFamily())
                    .build());

            currentTime = slotEnd;
            lastFamily = order.getColorFamily();
        }
        return slots;
    }

    /**
     * Final Evaluation Function
     */
    public Schedule evaluateSchedule(List<ScheduleSlot> slots) {
        int totalCleaning = slots.stream().mapToInt(ScheduleSlot::getCleaningBeforeMinutes).sum();
        long totalProd = slots.stream()
                .mapToLong(s -> java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                .sum();

        char ecoGrade = ecoEfficiencyService.calculateEcoGrade(totalCleaning, (int) totalProd);
        int waterSaved = ecoEfficiencyService.calculateWaterUsage(totalCleaning); // Here we would compare vs FIFO
        double wasteSaved = ecoEfficiencyService.calculateChemicalWaste(totalCleaning);

        return Schedule.builder()
                .generatedAt(LocalDateTime.now())
                .totalCleaningTimeMinutes(totalCleaning)
                .ecoGrade(ecoGrade)
                .totalWaterSavedLiters(waterSaved)
                .totalChemicalWasteSavedKg(wasteSaved)
                .slots(slots)
                .build();
    }

    /**
     * Orchestrates the full 4-phase optimization
     */
    public Schedule generateOptimizedSchedule(List<Order> orders) {
        List<Order> analyzed = analyzeOrders(orders);
        List<Order> baseSeq = generateBaseSchedule(analyzed);
        List<Order> optimizedSeq = optimizeSequence(baseSeq);
        // Phase 4: Simulated Annealing (Simplified for MVP)
        // In 100 iterations, we'd swap non-critical orders and keep if score improves

        List<ScheduleSlot> slots = mapToSlots(optimizedSeq, LocalDateTime.now().withHour(startHour).withMinute(0));
        Schedule schedule = evaluateSchedule(slots);

        int fifoCleaning = calculateFifoCleaningTime(analyzed);
        schedule.setFifoCleaningTimeMinutes(fifoCleaning);

        return schedule;
    }

    private int calculateFifoCleaningTime(List<Order> orders) {
        int total = 0;
        ColorFamily last = ColorFamily.WHITES_PASTELS;
        for (Order o : orders) {
            total += cleaningService.calculateCleaningTime(last, o.getColorFamily());
            last = o.getColorFamily();
        }
        return total;
    }

    public ScheduleResponseDTO convertToDTO(Schedule schedule) {
        int timeSaved = schedule.getFifoCleaningTimeMinutes() - schedule.getTotalCleaningTimeMinutes();

        // Calculate Deadline Compliance
        long totalOrders = schedule.getSlots().size();
        long compliantOrders = schedule.getSlots().stream()
                .filter(s -> {
                    LocalDateTime completionTime = s.getEndTime();
                    LocalDateTime deadlineTime = s.getOrder().getCreatedAt().plusHours(s.getOrder().getDeadlineHours());
                    return completionTime.isBefore(deadlineTime);
                }).count();
        double complianceRate = totalOrders > 0 ? (double) compliantOrders / totalOrders * 100 : 100.0;

        // Calculate Machine Efficiency
        long totalProdMinutes = schedule.getSlots().stream()
                .mapToLong(s -> java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                .sum();
        long totalDowntimeMinutes = schedule.getTotalCleaningTimeMinutes() + (totalOrders * setupTimeMinutes);
        double efficiency = totalProdMinutes > 0
                ? (double) totalProdMinutes / (totalProdMinutes + totalDowntimeMinutes) * 100
                : 0;

        return ScheduleResponseDTO.builder()
                .optimizedCleaningTimeMinutes(schedule.getTotalCleaningTimeMinutes())
                .fifoCleaningTimeMinutes(schedule.getFifoCleaningTimeMinutes())
                .timeSavedMinutes(Math.max(0, timeSaved))
                .deadlineCompliance(String.format("%.1f%%", complianceRate))
                .machineEfficiency(String.format("%.1f%%", efficiency))
                .ecoGrade(schedule.getEcoGrade())
                .waterSavedLiters(schedule.getTotalWaterSavedLiters())
                .chemicalWasteSavedKg(schedule.getTotalChemicalWasteSavedKg())
                .schedule(schedule.getSlots().stream().map(s -> ScheduleResponseDTO.SlotDTO.builder()
                        .orderId(s.getOrder().getId())
                        .colorFamily(s.getColorFamily())
                        .startTime(s.getStartTime().toString()) // Full ISO for JS parsing
                        .endTime(s.getEndTime().toString())
                        .cleaningBeforeMinutes(s.getCleaningBeforeMinutes())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
