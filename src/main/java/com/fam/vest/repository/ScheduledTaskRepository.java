package com.fam.vest.repository;

import com.fam.vest.entity.ScheduledTask;
import com.fam.vest.enums.SCHEDULER;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

    Optional<ScheduledTask> findBySchedulerName(SCHEDULER schedulerName);

    List<ScheduledTask> findAllByOrderBySchedulerNameAsc();
}
