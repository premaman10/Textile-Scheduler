package com.rainbow.scheduler.repository;

import com.rainbow.scheduler.model.SimulationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {
    List<SimulationRun> findTop10ByOrderByTimestampDesc();
}
