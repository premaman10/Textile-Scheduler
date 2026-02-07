package com.rainbow.scheduler.repository;

import com.rainbow.scheduler.model.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {
}
