package com.fam.vest.schedulers;

import com.fam.vest.entity.ScheduledTask;
import com.fam.vest.enums.SCHEDULER;
import com.fam.vest.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerExecutionService {

    private final ScheduledTaskRepository repository;

    private enum SCHEDULER_STATUS {
        IN_PROGRESS, COMPLETED, FAILED, SKIPPED
    }

    public void execute(SCHEDULER schedulerName, Runnable task) {
        log.info("SCHEDULER_EXEC_START | Scheduler: {} | StartTime: {}",
                schedulerName, new Date());

        Optional<ScheduledTask> scheduledTaskOptional = repository.findBySchedulerName(schedulerName);
        if (scheduledTaskOptional.isEmpty()) {
            log.warn("SCHEDULER_EXEC_ERROR | Scheduler: {} | Error: NOT_FOUND_IN_DB | Action: SKIPPING",
                    schedulerName);
            return;
        }

        ScheduledTask scheduledTask = scheduledTaskOptional.get();
        log.debug("SCHEDULER_EXEC_INFO | Scheduler: {} | TaskId: {} | IsActive: {} | LastExecution: {}",
                schedulerName, scheduledTask.getId(), scheduledTask.getIsActive(), scheduledTask.getLastExecutionDate());

        if (!Boolean.TRUE.equals(scheduledTask.getIsActive())) {
            log.info("SCHEDULER_EXEC_SKIP | Scheduler: {} | Reason: INACTIVE",
                    schedulerName);
            this.updateTaskStatus(scheduledTask, SCHEDULER_STATUS.SKIPPED.toString(), null);
            return;
        }

        // Start execution
        this.startTaskStatus(scheduledTask, SCHEDULER_STATUS.IN_PROGRESS.toString());

        try {
            log.info("SCHEDULER_EXEC_RUNNING | Scheduler: {} | TaskStartTime: {}",
                    schedulerName, new Date());

            // Special handling for restart application
            if (schedulerName.equals(SCHEDULER.RESTART_APPLICATION)) {
                log.info("SCHEDULER_EXEC_SPECIAL | Scheduler: {} | Action: RESTART_APP_HANDLER",
                        schedulerName);
                this.updateTaskStatus(scheduledTask, SCHEDULER_STATUS.COMPLETED.toString(), null);
            }

            // Execute the task
            task.run();

            log.info("SCHEDULER_EXEC_SUCCESS | Scheduler: {} | EndTime: {}",
                    schedulerName, new Date());
            this.updateTaskStatus(scheduledTask, SCHEDULER_STATUS.COMPLETED.toString(), null);

        } catch (Exception ex) {
            log.error("SCHEDULER_EXEC_FAILED | Scheduler: {} | Error: {} | ErrorMessage: {}",
                    schedulerName, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            this.updateTaskStatus(scheduledTask, SCHEDULER_STATUS.FAILED.toString(), ex.getMessage());
        }
    }

    /**
     * Starts task status with initial values and saves to repository
     */
    private void startTaskStatus(ScheduledTask task, String status) {
        Date start = new Date();
        log.info("SCHEDULER_TASK_START | Scheduler: {} | TaskId: {} | Status: {} | StartTime: {}",
                task.getSchedulerName(), task.getId(), status, start);

        task.setLastExecutionDate(start);
        task.setExecutionStartTime(start);
        task.setExecutionEndTime(null);
        task.setStatus(SCHEDULER_STATUS.IN_PROGRESS.toString());

        try {
            repository.save(task);
            log.debug("SCHEDULER_TASK_SAVED | Scheduler: {} | TaskId: {} | Action: START_STATUS_SAVED",
                    task.getSchedulerName(), task.getId());
        } catch (Exception ex) {
            log.error("SCHEDULER_TASK_SAVE_ERROR | Scheduler: {} | TaskId: {} | Error: {}",
                    task.getSchedulerName(), task.getId(), ex.getMessage(), ex);
        }
    }

    /**
     * Updates task status, error message, and end time, then saves to repository
     */
    private void updateTaskStatus(ScheduledTask task, String status, String errorMessage) {
        Date endTime = new Date();

        log.info("SCHEDULER_TASK_UPDATE | Scheduler: {} | TaskId: {} | Status: {} | EndTime: {} | HasError: {}",
                task.getSchedulerName(), task.getId(), status, endTime, errorMessage != null);

        if (errorMessage != null) {
            log.warn("SCHEDULER_TASK_ERROR_DETAIL | Scheduler: {} | ErrorMessage: {}",
                    task.getSchedulerName(), errorMessage);
        }

        task.setStatus(status);
        task.setErrorMessage(errorMessage);
        task.setExecutionEndTime(endTime);

        try {
            repository.save(task);
            log.debug("SCHEDULER_TASK_SAVED | Scheduler: {} | TaskId: {} | Action: FINAL_STATUS_SAVED",
                    task.getSchedulerName(), task.getId());
        } catch (Exception ex) {
            log.error("SCHEDULER_TASK_SAVE_ERROR | Scheduler: {} | TaskId: {} | Error: {}",
                    task.getSchedulerName(), task.getId(), ex.getMessage(), ex);
        }
    }
}
