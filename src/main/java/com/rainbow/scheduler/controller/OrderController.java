package com.rainbow.scheduler.controller;

import com.rainbow.scheduler.model.Order;
import com.rainbow.scheduler.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Order::getScheduledStartTime,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(Order::getId))
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        if (orderRepository.count() >= 1000) {
            return ResponseEntity.badRequest()
                    .body("Factory capacity reached (1000 orders). Please clear or process existing orders.");
        }

        if (order.getQuantityMeters() < 100) {
            return ResponseEntity.badRequest().body("Minimum order size is 100 meters.");
        }

        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearOrders() {
        orderRepository.deleteAll();
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id,
            @RequestParam com.rainbow.scheduler.model.OrderStatus status) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);
            return ResponseEntity.ok(orderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }
}
