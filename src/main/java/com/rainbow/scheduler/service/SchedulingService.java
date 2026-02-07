package com.rainbow.scheduler.service;

import com.rainbow.scheduler.model.*;
import com.rainbow.scheduler.repository.OrderRepository;
import com.rainbow.scheduler.repository.SimulationRunRepository;
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
    private final SimulationRunRepository simulationRunRepository;
    private final OrderRepository orderRepository;

    @Value("${production.dyeing-speed-meters-per-hour:50}")
    private int dyeingSpeed;

    @Value("${production.setup-time-minutes:15}")
    private int setupTimeMinutes;

    @Value("${production.start-hour:8}")
    private int startHour;

    @Value("${production.daily-window-hours:16}")
    private int dailyWindowHours;

    public List<Order> analyzeOrders(List<Order> orders) {
        return orders.stream().map(order -> {
            double prodTimeHours = (double) order.getQuantityMeters() / dyeingSpeed;
            order.setProductionTimeHours(prodTimeHours);
            double urgency = (order.getQuantityMeters() * order.getOrderType().getPriceMultiplier())
                    / order.getDeadlineHours();
            order.setUrgencyScore(urgency);
            order.setCritical(order.getDeadlineHours() < 12 || order.getOrderType() == OrderType.RUSH);
            return order;
        }).collect(Collectors.toList());
    }

    public List<Order> generateBaseSchedule(List<Order> analyzedOrders) {
        List<Order> critical = analyzedOrders.stream()
                .filter(Order::isCritical)
                .sorted(Comparator.comparingInt(Order::getDeadlineHours))
                .collect(Collectors.toList());

        List<Order> others = analyzedOrders.stream()
                .filter(o -> !o.isCritical())
                .sorted(Comparator.comparing(Order::getColorFamily).thenComparing(o -> o.getOrderType().ordinal()))
                .collect(Collectors.toList());

        List<Order> baseSeq = new ArrayList<>(critical);
        baseSeq.addAll(others);
        return baseSeq;
    }

    public List<Order> optimizeSequence(List<Order> baseSeq) {
        LinkedList<Order> optimized = new LinkedList<>();
        if (baseSeq.isEmpty())
            return baseSeq;

        for (Order order : baseSeq) {
            if (optimized.isEmpty()) {
                optimized.add(order);
                continue;
            }
            int bestPos = optimized.size();
            int minAddedCleaning = cleaningService.calculateCleaningTime(optimized.getLast().getColorFamily(),
                    order.getColorFamily());
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
                    if (!order.isCritical() || i < 5) {
                        minAddedCleaning = extraCleaning;
                        bestPos = i;
                    }
                }
            }
            optimized.add(bestPos, order);
        }
        return new ArrayList<>(optimized);
    }

    public List<ScheduleSlot> mapToSlots(List<Order> sequence, LocalDateTime startTime) {
        List<ScheduleSlot> slots = new ArrayList<>();
        LocalDateTime currentTime = startTime;
        ColorFamily lastFamily = ColorFamily.WHITES_PASTELS;

        for (Order order : sequence) {
            int cleaningTime = cleaningService.calculateCleaningTime(lastFamily, order.getColorFamily());
            currentTime = currentTime.plusMinutes(cleaningTime + setupTimeMinutes);
            LocalDateTime slotStart = currentTime;
            long prodMinutes = (long) (order.getProductionTimeHours() * 60);
            LocalDateTime slotEnd = slotStart.plusMinutes(prodMinutes);

            // Persist for history
            order.setScheduledStartTime(slotStart);
            order.setScheduledEndTime(slotEnd);

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

    public Schedule evaluateSchedule(List<ScheduleSlot> slots) {
        int totalCleaning = slots.stream().mapToInt(ScheduleSlot::getCleaningBeforeMinutes).sum();
        long totalProd = slots.stream()
                .mapToLong(s -> java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();
        char ecoGrade = ecoEfficiencyService.calculateEcoGrade(totalCleaning, (int) totalProd);
        int waterSaved = ecoEfficiencyService.calculateWaterUsage(totalCleaning);
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

    public Schedule generateOptimizedSchedule(List<Order> orders) {
        List<Order> analyzed = analyzeOrders(orders);
        List<Order> optimizedSeq = optimizeSequence(generateBaseSchedule(analyzed));
        List<ScheduleSlot> slots = mapToSlots(optimizedSeq, LocalDateTime.now().withHour(startHour).withMinute(0));
        Schedule schedule = evaluateSchedule(slots);
        schedule.setFifoCleaningTimeMinutes(calculateFifoCleaningTime(analyzed));

        // Save orders to persist the scheduledStartTime/EndTime
        orderRepository.saveAll(optimizedSeq);

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
        Map<String, String> metrics = calculateMetrics(schedule);

        if (!schedule.getSlots().isEmpty() && schedule.getSlots().get(0).getOrder().getSimulationRunId() != null) {
            archiveSimulationRun(schedule.getSlots().get(0).getOrder().getSimulationRunId(), schedule, metrics);
        }

        return ScheduleResponseDTO.builder()
                .optimizedCleaningTimeMinutes(schedule.getTotalCleaningTimeMinutes())
                .fifoCleaningTimeMinutes(schedule.getFifoCleaningTimeMinutes())
                .timeSavedMinutes(Math.max(0, timeSaved))
                .deadlineCompliance(metrics.get("compliance"))
                .machineEfficiency(metrics.get("efficiency"))
                .ecoGrade(schedule.getEcoGrade())
                .waterSavedLiters(schedule.getTotalWaterSavedLiters())
                .chemicalWasteSavedKg(schedule.getTotalChemicalWasteSavedKg())
                .schedule(schedule.getSlots().stream().map(s -> ScheduleResponseDTO.SlotDTO.builder()
                        .orderId(s.getOrder().getId())
                        .colorFamily(s.getColorFamily())
                        .startTime(s.getStartTime().toString())
                        .endTime(s.getEndTime().toString())
                        .cleaningBeforeMinutes(s.getCleaningBeforeMinutes())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private Map<String, String> calculateMetrics(Schedule schedule) {
        long totalOrders = schedule.getSlots().size();
        long compliantOrders = schedule.getSlots().stream()
                .filter(s -> s.getEndTime()
                        .isBefore(s.getOrder().getCreatedAt().plusHours(s.getOrder().getDeadlineHours())))
                .count();
        double complianceRate = totalOrders > 0 ? (double) compliantOrders / totalOrders * 100 : 100.0;
        long totalProdMinutes = schedule.getSlots().stream()
                .mapToLong(s -> java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).sum();
        long totalDowntimeMinutes = schedule.getTotalCleaningTimeMinutes() + (totalOrders * setupTimeMinutes);
        double efficiency = totalProdMinutes > 0
                ? (double) totalProdMinutes / (totalProdMinutes + totalDowntimeMinutes) * 100
                : 0;

        Map<String, String> m = new HashMap<>();
        m.put("compliance", String.format("%.1f%%", complianceRate));
        m.put("efficiency", String.format("%.1f%%", efficiency));
        return m;
    }

    private void archiveSimulationRun(Long runId, Schedule schedule, Map<String, String> metrics) {
        simulationRunRepository.findById(runId).ifPresent(run -> {
            run.setDeadlineCompliance(metrics.get("compliance"));
            run.setMachineEfficiency(metrics.get("efficiency"));
            run.setTotalCleaningTimeMinutes(schedule.getTotalCleaningTimeMinutes());
            run.setTimeSavedMinutes(schedule.getFifoCleaningTimeMinutes() - schedule.getTotalCleaningTimeMinutes());
            run.setEcoGrade(schedule.getEcoGrade());
            simulationRunRepository.save(run);
        });
    }
}
