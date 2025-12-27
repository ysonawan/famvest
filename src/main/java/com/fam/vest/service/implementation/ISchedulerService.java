package com.fam.vest.service.implementation;

import com.fam.vest.dto.request.StatusUpdateRequest;
import com.fam.vest.entity.ScheduledTask;
import com.fam.vest.enums.SCHEDULER;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.repository.ScheduledTaskRepository;
import com.fam.vest.schedulers.CustomSchedulers;
import com.fam.vest.service.SchedulerService;
import com.fam.vest.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ISchedulerService implements SchedulerService {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final CustomSchedulers customSchedulers;

    @Override
    public List<ScheduledTask> getAllScheduledTasks() {
        return scheduledTaskRepository.findAllByOrderBySchedulerNameAsc();
    }

    @Override
    public ScheduledTask getScheduledTask(String idOrName) {
        Optional<ScheduledTask> scheduledTask;
        if (CommonUtil.isNumeric(idOrName)) {
            Long id = Long.parseLong(idOrName);
            scheduledTask = scheduledTaskRepository.findById(id);
        } else {
            SCHEDULER schedulerName = SCHEDULER.fromString(idOrName.toUpperCase());
            scheduledTask = scheduledTaskRepository.findBySchedulerName(schedulerName);
        }
        return scheduledTask.orElseThrow(() -> new ResourceNotFoundException("Scheduled task not found for idOrName: " + idOrName));

    }

    @Override
    public String executeScheduledTask(String idOrName) {
        ScheduledTask scheduledTask = this.getTask(idOrName);
        log.info("Executing scheduled task: {}", scheduledTask.getSchedulerName());
        try {
            this.execute(scheduledTask.getSchedulerName());
            return "Scheduled task executed successfully.";
        } catch (Exception e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error executing scheduled task for: {}. Error: {}", scheduledTask.getSchedulerName(), errorMessage, e);
            throw new InternalException(errorMessage);
        }
    }

    private ScheduledTask getTask(String idOrName) {
        ScheduledTask scheduledTask;
        if (CommonUtil.isNumeric(idOrName)) {
            Long id = Long.parseLong(idOrName);
            scheduledTask = scheduledTaskRepository.findById(id).orElse(null);
        } else {
            SCHEDULER schedulerName = SCHEDULER.fromString(idOrName.toUpperCase());
            scheduledTask = scheduledTaskRepository.findBySchedulerName(schedulerName).orElse(null);
        }
        if(null == scheduledTask) {
            throw new ResourceNotFoundException("Scheduled task not found with idOrName: " + idOrName);
        }
        return scheduledTask;
    }

    @Override
    public ScheduledTask updateScheduledTaskStatus(String idOrName, StatusUpdateRequest statusUpdateRequest) {
        ScheduledTask scheduledTask = this.getTask(idOrName);
        scheduledTask.setIsActive(statusUpdateRequest.getStatus().equalsIgnoreCase("ACTIVATE"));
        return scheduledTaskRepository.save(scheduledTask);
    }

    private void execute(SCHEDULER schedulerName) {
        switch (schedulerName) {
            case RELOAD_INSTRUMENTS:
                customSchedulers.reloadInstruments();
                break;
            case RELOAD_WATCHLIST:
                customSchedulers.reloadWatchlist();
                break;
            case CLEAR_MF_NAV_CACHE:
                customSchedulers.clearMutualFundNavCache();
                break;
            case UPDATE_MF_NAV_CACHE:
                customSchedulers.updateMutualFundNavCache();
                break;
            case CAPTURE_ACCOUNT_SNAPSHOT:
                customSchedulers.captureAccountSnapshot();
                break;
            case WEEKLY_PF_REPORT:
                customSchedulers.generateAndNotifyWeeklyPortfolioReport();
                break;
            case MONTHLY_PF_REPORT:
                customSchedulers.generateAndNotifyMonthlyPortfolioReport();
                break;
            case QUARTERLY_PF_REPORT:
                customSchedulers.generateAndNotifyQuarterlyPortfolioReport();
                break;
            case YEARLY_PF_REPORT:
                customSchedulers.generateAndNotifyYearlyPortfolioReport();
                break;
            case MONTHLY_SIP_REPORT:
                customSchedulers.generateAndNotifyMonthlySipReport();
                break;
            case RESTART_APPLICATION:
                customSchedulers.restartApplication();
                break;
            case SCHEDULER_ERROR_NOTIFICATION:
                customSchedulers.notifySchedulerErrors();
                break;
            case RETRIEVE_IPO_DETAILS:
                customSchedulers.refreshIposFromKiteInternalApi();
                break;
            case OPEN_IPO_NOTIFICATION:
                customSchedulers.notifyOpenIpos();
                break;
            default:
                throw new ResourceNotFoundException("No scheduler found for: " + schedulerName);
        }
    }
}
