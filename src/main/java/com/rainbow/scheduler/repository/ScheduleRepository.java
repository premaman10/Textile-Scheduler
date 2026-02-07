package com.rainbow.scheduler.repository;

import com.rainbow.scheduler.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}
