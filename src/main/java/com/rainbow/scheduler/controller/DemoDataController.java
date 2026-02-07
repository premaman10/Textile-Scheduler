package com.rainbow.scheduler.controller;

import com.rainbow.scheduler.model.ColorFamily;
import com.rainbow.scheduler.model.Order;
import com.rainbow.scheduler.model.OrderType;
import com.rainbow.scheduler.repository.OrderRepository;
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
    private final Random random = new Random();

    @PostMapping("/populate")
    public String populateData() {
        List<Order> orders = new ArrayList<>();
        ColorFamily[] families = ColorFamily.values();
        OrderType[] types = OrderType.values();

        for (int i = 0; i < 100; i++) {
            orders.add(Order.builder()
                    .colorName("Color-" + (random.nextInt(50) + 1))
                    .colorFamily(families[random.nextInt(families.length)])
                    .quantityMeters(100 + random.nextInt(901)) // 100-1000
                    .orderType(types[random.nextInt(types.length)])
                    .deadlineHours(8 + random.nextInt(64)) // 8 to 72 hours
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        orderRepository.saveAll(orders);
        return "Successfully added 100 demo orders.";
    }
}
