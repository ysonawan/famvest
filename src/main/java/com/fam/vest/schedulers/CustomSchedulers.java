package com.fam.vest.schedulers;

import com.fam.vest.cache.MutualFundNavCacheService;
import com.fam.vest.enums.SCHEDULER;
import com.fam.vest.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomSchedulers {

    private final InstrumentService instrumentService;
    private final WatchlistService watchlistService;
    private final MutualFundNavCacheService mutualFundNavCacheService;
    private final SnapshotService snapshotService;
    private final HoldingService holdingService;
    private final MutualFundService mutualFundService;
    private final SchedulerExecutionService schedulerExecutionService;
    private final SchedulerConfigurationService schedulerConfigurationService;
    private final AdminService adminService;
    private final IpoService ipoService;

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('RELOAD_INSTRUMENTS')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('RELOAD_INSTRUMENTS')}")
    public void reloadInstruments() {
        schedulerExecutionService.execute(SCHEDULER.RELOAD_INSTRUMENTS, instrumentService::fetchAndSaveInstruments);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('RELOAD_WATCHLIST')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('RELOAD_WATCHLIST')}")
    public void reloadWatchlist() {
        schedulerExecutionService.execute(SCHEDULER.RELOAD_WATCHLIST, watchlistService::reloadWatchlist);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('CLEAR_MF_NAV_CACHE')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('CLEAR_MF_NAV_CACHE')}")
    public void clearMutualFundNavCache() {
        schedulerExecutionService.execute(SCHEDULER.CLEAR_MF_NAV_CACHE, mutualFundNavCacheService::emptyMutualFundNavCache);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('UPDATE_MF_NAV_CACHE')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('UPDATE_MF_NAV_CACHE')}")
    public void updateMutualFundNavCache() {
        schedulerExecutionService.execute(SCHEDULER.UPDATE_MF_NAV_CACHE, mutualFundNavCacheService::updateMutualFundNavCache);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('CAPTURE_ACCOUNT_SNAPSHOT')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('CAPTURE_ACCOUNT_SNAPSHOT')}")
    public void captureAccountSnapshot() {
        schedulerExecutionService.execute(SCHEDULER.CAPTURE_ACCOUNT_SNAPSHOT, snapshotService::captureSnapshot);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('WEEKLY_PF_REPORT')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('WEEKLY_PF_REPORT')}")
    public void generateAndNotifyWeeklyPortfolioReport() {
        schedulerExecutionService.execute(SCHEDULER.WEEKLY_PF_REPORT, holdingService::generateAndNotifyWeeklyPortfolioReport);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('MONTHLY_SIP_REPORT')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('MONTHLY_SIP_REPORT')}")
    public void generateAndNotifyMonthlySipReport() {
        schedulerExecutionService.execute(SCHEDULER.MONTHLY_SIP_REPORT, mutualFundService::generateAndNotifyMonthlySipReport);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('MONTHLY_PF_REPORT')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('MONTHLY_PF_REPORT')}")
    public void generateAndNotifyMonthlyPortfolioReport() {
        schedulerExecutionService.execute(SCHEDULER.MONTHLY_PF_REPORT, holdingService::generateAndNotifyMonthlyPortfolioReport);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('QUARTERLY_PF_REPORT')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('QUARTERLY_PF_REPORT')}")
    public void generateAndNotifyQuarterlyPortfolioReport() {
        schedulerExecutionService.execute(SCHEDULER.QUARTERLY_PF_REPORT, holdingService::generateAndNotifyQuarterlyPortfolioReport);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('YEARLY_PF_REPORT')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('YEARLY_PF_REPORT')}")
    public void generateAndNotifyYearlyPortfolioReport() {
        schedulerExecutionService.execute(SCHEDULER.YEARLY_PF_REPORT, holdingService::generateAndNotifyYearlyPortfolioReport);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('RESTART_APPLICATION')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('RESTART_APPLICATION')}")
    public void restartApplication() {
        schedulerExecutionService.execute(SCHEDULER.RESTART_APPLICATION, adminService::restartApplication);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('SCHEDULER_ERROR_NOTIFICATION')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('SCHEDULER_ERROR_NOTIFICATION')}")
    public void notifySchedulerErrors() {
        schedulerExecutionService.execute(SCHEDULER.SCHEDULER_ERROR_NOTIFICATION, adminService::notifySchedulerErrors);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('RETRIEVE_IPO_DETAILS')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('RETRIEVE_IPO_DETAILS')}")
    public void refreshIposFromKiteInternalApi() {
        schedulerExecutionService.execute(SCHEDULER.RETRIEVE_IPO_DETAILS, ipoService::refreshIposFromKiteInternalApi);
    }

    @Scheduled(cron = "#{@schedulerConfigurationService.getCronExpression('OPEN_IPO_NOTIFICATION')}", zone = "#{@schedulerConfigurationService.getCronTimeZone('OPEN_IPO_NOTIFICATION')}")
    public void notifyOpenIpos() {
        schedulerExecutionService.execute(SCHEDULER.OPEN_IPO_NOTIFICATION, ipoService::notifyOpenIpos);
    }
}
