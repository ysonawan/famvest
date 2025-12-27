package com.fam.vest.schedulers;

import com.fam.vest.entity.ScheduledTask;
import com.fam.vest.enums.SCHEDULER;
import com.fam.vest.pojo.records.CronExpression;
import com.fam.vest.repository.ScheduledTaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfigurationService {

    private final ScheduledTaskRepository scheduledTaskRepository;

    private final Map<SCHEDULER, CronExpression> schedulerCronExpressionMap = new HashMap<>(10);

    @PostConstruct
    public void init() {
        log.info("SchedulerConfigurationService initialized. Getting scheduler cron expressions...");
        List<ScheduledTask> scheduledTasks = scheduledTaskRepository.findAllByOrderBySchedulerNameAsc();
        scheduledTasks.forEach(scheduler -> {
            log.info("Setting scheduler cron expression for {}", scheduler);
            schedulerCronExpressionMap.put(scheduler.getSchedulerName(), new CronExpression(scheduler.getCronExpression(), scheduler.getTimeZone()));
        });
    }

    public String getCronExpression(String schedulerName) {
        return schedulerCronExpressionMap.get(SCHEDULER.fromString(schedulerName)).expression();
    }

    public String getCronTimeZone(String schedulerName) {
        return schedulerCronExpressionMap.get(SCHEDULER.fromString(schedulerName)).timeZone();
    }
}
