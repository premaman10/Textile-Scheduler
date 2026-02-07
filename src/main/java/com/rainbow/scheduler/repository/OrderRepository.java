package com.rainbow.scheduler.repository;

import com.rainbow.scheduler.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
