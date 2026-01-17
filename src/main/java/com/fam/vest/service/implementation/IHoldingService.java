package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.entity.UserPreferences;
import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.pojo.email.HoldingReportRow;
import com.fam.vest.entity.AccountSnapshot;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.pojo.HoldingComparisonReport;
import com.fam.vest.dto.response.GainersLosersResponse;
import com.fam.vest.dto.response.HoldingComparisonDetails;
import com.fam.vest.dto.response.HoldingDetails;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.repository.AccountSnapshotRepository;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.UserPreferencesRepository;
import com.fam.vest.service.EmailService;
import com.fam.vest.service.HoldingService;
import com.fam.vest.service.MutualFundService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IHoldingService implements HoldingService {

    private final TradingAccountService tradingAccountService;
    private final KiteConnector kiteConnector;
    private final MutualFundService mutualFundService;
    private final AccountSnapshotRepository accountSnapshotRepository;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final ApplicationUserRepository applicationUserRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    @Value("${fam.vest.app.domain}")
    private String applicationDomain;

    /**
     * Hardcoded mapping of cross-exchange instruments that represent the same security.
     * Key is the normalized identifier, Value is a set of instrument identifiers across exchanges
     */
    private static final Map<String, Set<String>> CROSS_EXCHANGE_INSTRUMENT_MAPPING = new HashMap<>();

    static {
        // Add mapping for instruments that are same but listed on different exchanges
        // Format: "754GS2036" maps to both NSE "754GS2036" and BSE "754GS2036-GS"
        CROSS_EXCHANGE_INSTRUMENT_MAPPING.put("754GS2036", Set.of("754GS2036", "754GS2036-GS"));

        // Add more mappings as needed
        // CROSS_EXCHANGE_INSTRUMENT_MAPPING.put("ISIN_OR_BASE", Set.of("INSTRUMENT_ID1", "INSTRUMENT_ID2"));
    }

    @Override
    public List<HoldingDetails> getAllHoldings() {
        return this.getHoldings(null, Optional.ofNullable(null), Optional.ofNullable(null));
    }

    @Override
    public List<HoldingDetails> getHoldings(UserDetails userDetails, Optional<String> type, Optional<String> tradingAccountId) {
        List<TradingAccount> tradingAccounts = null;
        if(null == userDetails) {
            tradingAccounts = tradingAccountService.getAllTradingAccounts();
        } else {
            tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        }
        if (tradingAccountId.isPresent()) {
            tradingAccounts = tradingAccounts.stream().
                    filter(tradingAccount -> tradingAccount.getUserId().equals(tradingAccountId.get())).toList();
        }
        List<HoldingDetails> holdingDetails = new ArrayList<>();
        AtomicLong sequenceNumber = new AtomicLong(1);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                if (type.isEmpty()) {
                    this.getStockHoldings(kiteConnect, holdingDetails, sequenceNumber);
                    mutualFundService.getMutualFundHoldings(tradingAccount.getUserId(), kiteConnect, holdingDetails, sequenceNumber);
                } else if (type.get().equalsIgnoreCase("stocks")) {
                    this.getStockHoldings(kiteConnect, holdingDetails, sequenceNumber);
                } else if (type.get().equalsIgnoreCase("mf")) {
                    mutualFundService.getMutualFundHoldings(tradingAccount.getUserId(), kiteConnect, holdingDetails, sequenceNumber);
                }
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting holdings for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return holdingDetails;
    }

    private void getStockHoldings(KiteConnect kiteConnect, List<HoldingDetails> holdingDetails, AtomicLong sequenceNumber)
            throws KiteException, IOException {
        kiteConnect.getHoldings().forEach(holding -> {
            HoldingDetails holdingDetail = new HoldingDetails();
            holdingDetail.setType("Stocks");
            holdingDetail.setUserId(kiteConnect.getUserId());
            holdingDetail.setInstrument(holding.tradingSymbol);
            holdingDetail.setTradingSymbol(holding.tradingSymbol);
            holdingDetail.setInstrumentToken(Long.valueOf(holding.instrumentToken));
            Double quantity = Double
                    .valueOf(holding.quantity + Integer.valueOf(holding.collateralQuantity) + holding.t1Quantity);
            holdingDetail.setQuantity(quantity);
            holdingDetail.setAveragePrice(holding.averagePrice);
            holdingDetail.setLastPrice(holding.lastPrice);
            holdingDetail.setInvestedAmount(holdingDetail.getQuantity() * holdingDetail.getAveragePrice());
            holdingDetail.setCurrentValue(holdingDetail.getQuantity() * holdingDetail.getLastPrice());
            holdingDetail.setDayChange(holding.dayChange);
            holdingDetail.setDayChangePercentage(holding.dayChangePercentage);
            holdingDetail.setNetPnl(holdingDetail.getCurrentValue() - holdingDetail.getInvestedAmount());
            if(holdingDetail.getInvestedAmount() == null || holdingDetail.getInvestedAmount() == 0) {
                holdingDetail.setNetChangePercentage(0.0);
            } else {
                holdingDetail.setNetChangePercentage((holdingDetail.getNetPnl() * 100.0) / holdingDetail.getInvestedAmount());
            }
            if(holdingDetail.getCurrentValue() == null || holdingDetail.getCurrentValue() == 0) {
                holdingDetail.setDayChangePercentage(0.0);
            } else {
                holdingDetail.setDayPnl(holdingDetail.getDayChangePercentage() * holdingDetail.getCurrentValue() / 100);
            }
            holdingDetail.setSequenceNumber(sequenceNumber.getAndIncrement());
            holdingDetail.setProduct(holding.product);
            holdingDetail.setPrice(holding.price);
            holdingDetail.setT1Quantity(holding.t1Quantity);
            holdingDetail.setCollateralQuantity(holding.collateralQuantity);
            holdingDetail.setCollateralType(holding.collateraltype);
            holdingDetail.setIsin(holding.isin);
            holdingDetail.setPnl(holding.pnl);
            holdingDetail.setRealisedQuantity(holding.realisedQuantity);
            holdingDetail.setExchange(holding.exchange);
            holdingDetail.setUsedQuantity(holding.usedQuantity);
            holdingDetail.setAuthorisedQuantity(holding.authorisedQuantity);
            holdingDetail.setAuthorisedDate(holding.authorisedDate);
            holdingDetail.setDiscrepancy(holding.discrepancy);
            holdingDetails.add(holdingDetail);
        });
    }

    @Override
    public void generateAndNotifyWeeklyPortfolioReport() {
        try {
            log.info("Weekly portfolio report generation started");
            AccountSnapshot latestAccountSnapshot = accountSnapshotRepository.findLatestSnapshot(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
            Date lastSundayDate = CommonUtil.getLastSundayDate();
            AccountSnapshot lastWeekAccountSnapshot = accountSnapshotRepository.findAccountSnapshotBySnapshotDate(lastSundayDate).orElse(null);
            if(lastWeekAccountSnapshot == null || latestAccountSnapshot == null) {
                log.warn("Latest account snapshot or last sunday snapshot not fund. Skipping generation");
                return;
            }
            List<HoldingComparisonReport> report = this.generateHoldingComparisonReport(latestAccountSnapshot.getHoldings(), lastWeekAccountSnapshot.getHoldings());
            generateAndSendPortfolioReport(report, "weekly", DEFAULT_USER_PREFERENCES.WEEKLY_PORTFOLIO_REPORT, "Weekly Holdings Report");
            log.info("Weekly portfolio report generation completed");
        } catch (Exception exception) {
            log.error("Error while generating weekly portfolio report", exception);
            throw new InternalException("Error while generating weekly portfolio report: " + CommonUtil.getExceptionMessage(exception));
        }
    }

    @Override
    public void generateAndNotifyMonthlyPortfolioReport() {
        try {
            log.info("Monthly portfolio report generation started");
            AccountSnapshot latestAccountSnapshot = accountSnapshotRepository.findLatestSnapshot(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
            Date startOfLastMonth = CommonUtil.getStartOfLastMonth();
            AccountSnapshot lastMonthAccountSnapshot = accountSnapshotRepository.findAccountSnapshotBySnapshotDate(startOfLastMonth).orElse(null);
            if(lastMonthAccountSnapshot == null || latestAccountSnapshot == null) {
                log.warn("Latest account snapshot or start of last month snapshot not found. Skipping generation");
                return;
            }
            List<HoldingComparisonReport> report = this.generateHoldingComparisonReport(latestAccountSnapshot.getHoldings(), lastMonthAccountSnapshot.getHoldings());
            generateAndSendPortfolioReport(report, "monthly", DEFAULT_USER_PREFERENCES.MONTHLY_PORTFOLIO_REPORT, "Monthly Holdings Report");
            log.info("Monthly portfolio report generation completed");
        } catch (Exception exception) {
            log.error("Error while generating monthly portfolio report", exception);
            throw new InternalException("Error while generating monthly portfolio report: " + CommonUtil.getExceptionMessage(exception));
        }
    }

    @Override
    public void generateAndNotifyQuarterlyPortfolioReport() {
        try {
            log.info("Quarterly portfolio report generation started");
            AccountSnapshot latestAccountSnapshot = accountSnapshotRepository.findLatestSnapshot(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
            Date startOfLastQuarter = CommonUtil.getStartOfLastQuarter();
            AccountSnapshot lastQuarterAccountSnapshot = accountSnapshotRepository.findAccountSnapshotBySnapshotDate(startOfLastQuarter).orElse(null);
            if(lastQuarterAccountSnapshot == null || latestAccountSnapshot == null) {
                log.warn("Latest account snapshot or start of last quarter snapshot not found. Skipping generation");
                return;
            }
            List<HoldingComparisonReport> report = this.generateHoldingComparisonReport(latestAccountSnapshot.getHoldings(), lastQuarterAccountSnapshot.getHoldings());
            generateAndSendPortfolioReport(report, "quarterly", DEFAULT_USER_PREFERENCES.QUARTERLY_PORTFOLIO_REPORT, "Quarterly Holdings Report");
            log.info("Quarterly portfolio report generation completed");
        } catch (Exception exception) {
            log.error("Error while generating quarterly portfolio report", exception);
            throw new InternalException("Error while generating quarterly portfolio report: " + CommonUtil.getExceptionMessage(exception));
        }
    }

    @Override
    public void generateAndNotifyYearlyPortfolioReport() {
        try {
            log.info("Yearly portfolio report generation started");
            AccountSnapshot latestAccountSnapshot = accountSnapshotRepository.findLatestSnapshot(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
            Date startOfLastYear = CommonUtil.getStartOfLastYear();
            AccountSnapshot lastYearAccountSnapshot = accountSnapshotRepository.findAccountSnapshotBySnapshotDate(startOfLastYear).orElse(null);
            if(lastYearAccountSnapshot == null || latestAccountSnapshot == null) {
                log.warn("Latest account snapshot or start of last year snapshot not found. Skipping generation");
                return;
            }
            List<HoldingComparisonReport> report = this.generateHoldingComparisonReport(latestAccountSnapshot.getHoldings(), lastYearAccountSnapshot.getHoldings());
            generateAndSendPortfolioReport(report, "yearly", DEFAULT_USER_PREFERENCES.YEARLY_PORTFOLIO_REPORT, "Yearly Holdings Report");
            log.info("Yearly portfolio report generation completed");
        } catch (Exception exception) {
            log.error("Error while generating yearly portfolio report", exception);
            throw new InternalException("Error while generating yearly portfolio report: " + CommonUtil.getExceptionMessage(exception));
        }
    }

    @Override
    public GainersLosersResponse getGainersAndLosers(UserDetails userDetails, String timeframe, Optional<List<String>> tradingAccountIds) {
        // Calculate days based on timeframe
        int daysAgo = getTimeframeDays(timeframe);

        // Get target date for snapshot
        Date targetDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(targetDate);
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date snapshotDate = calendar.getTime();

        // Get historical snapshot
        Optional<AccountSnapshot> snapshotOpt = accountSnapshotRepository.findAccountSnapshotBySnapshotDate(snapshotDate);

        // If exact date not found, find the closest one
        if (snapshotOpt.isEmpty()) {
            List<AccountSnapshot> snapshots = accountSnapshotRepository.findBySnapshotDateGreaterThanEqual(snapshotDate);
            if (!snapshots.isEmpty()) {
                snapshotOpt = Optional.of(snapshots.get(0));
            }
        }
        List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        if(tradingAccountIds.isEmpty()) {
            tradingAccountIds = Optional.of(tradingAccounts.stream()
                    .map(TradingAccount::getUserId)
                    .toList());
        } else {
            //remove trading account id from the list which are not present in tradign accounts
            List<String> validTradingAccountIds = tradingAccounts.stream()
                    .map(TradingAccount::getUserId)
                    .toList();
            List<String> filteredTradingAccountIds = tradingAccountIds.get().stream()
                    .filter(validTradingAccountIds::contains)
                    .toList();
            tradingAccountIds = Optional.of(filteredTradingAccountIds);
        }
        List<String> userIds = tradingAccountIds.get();
        // Get current holdings - pass userIds for filtering
        List<HoldingDetails> currentHoldings = this.getHoldings(userDetails, Optional.empty(), Optional.empty());
        // filter current holdings by userIds
        currentHoldings = currentHoldings .stream().
                filter(holdingDetails -> userIds.contains(holdingDetails.getUserId())).
                toList();

        // Build response
        GainersLosersResponse.GainersLosersResponseBuilder responseBuilder = GainersLosersResponse.builder()
                .timeframe(timeframe)
                .currentDate(new Date())
                .currentHoldings(currentHoldings);

        if (snapshotOpt.isPresent()) {
            AccountSnapshot snapshot = snapshotOpt.get();
            List<HoldingDetails> snapshotHoldings = snapshot.getHoldings();

            // Filter snapshot holdings by user if specified
            snapshotHoldings = snapshotHoldings.stream()
                    .filter(h -> userIds.contains(h.getUserId()))
                    .toList();

            responseBuilder.snapshotDate(snapshot.getSnapshotDate())
                    .snapshotHoldings(snapshotHoldings);

            // Calculate gainers and losers
            List<HoldingComparisonDetails> comparisons = compareHoldings(snapshotHoldings, currentHoldings);

            // Sort and get top gainers
            List<HoldingComparisonDetails> topGainers = comparisons.stream()
                    .filter(c -> c.getPercentageChange() > 0)
                    .sorted((a, b) -> Double.compare(b.getPercentageChange(), a.getPercentageChange()))
                    .limit(5)
                    .toList();

            // Sort and get top losers
            List<HoldingComparisonDetails> topLosers = comparisons.stream()
                    .filter(c -> c.getPercentageChange() < 0)
                    .sorted(Comparator.comparingDouble(HoldingComparisonDetails::getPercentageChange))
                    .limit(5)
                    .toList();

            responseBuilder.topGainers(topGainers)
                    .topLosers(topLosers);
        }

        return responseBuilder.build();
    }

    private int getTimeframeDays(String timeframe) {
        return switch (timeframe) {
            case "1D" -> 1;
            case "1W" -> 7;
            case "1M" -> 30;
            case "3M" -> 90;
            case "6M" -> 180;
            case "1Y" -> 365;
            default -> 90; // Default to 3 months
        };
    }

    private List<HoldingComparisonDetails> compareHoldings(List<HoldingDetails> snapshotHoldings, List<HoldingDetails> currentHoldings) {
        // Group holdings by tradingSymbol to consolidate across accounts
        Map<String, ConsolidatedHolding> snapshotMap = consolidateHoldingsBySymbol(snapshotHoldings);
        Map<String, ConsolidatedHolding> currentMap = consolidateHoldingsBySymbol(currentHoldings);

        List<HoldingComparisonDetails> comparisons = new ArrayList<>();

        // Compare consolidated holdings
        for (Map.Entry<String, ConsolidatedHolding> entry : snapshotMap.entrySet()) {
            String tradingSymbol = entry.getKey();
            ConsolidatedHolding snapshotHolding = entry.getValue();
            ConsolidatedHolding currentHolding = currentMap.get(tradingSymbol);

            // Only compare if instrument exists in both snapshots
            if (currentHolding != null) {
                double previousPrice = snapshotHolding.avgPrice;
                double currentPrice = currentHolding.avgPrice;

                double absoluteChange = currentPrice - previousPrice;
                double percentageChange = previousPrice > 0 ? (absoluteChange / previousPrice) * 100 : 0.0;

                // Use minimum total quantity across all accounts
                double minQuantity = Math.min(snapshotHolding.totalQuantity, currentHolding.totalQuantity);

                if (minQuantity > 0) {
                    HoldingComparisonDetails comparison = HoldingComparisonDetails.builder()
                            .userId(snapshotHolding.userIds) // Comma-separated user IDs
                            .tradingSymbol(tradingSymbol)
                            .instrument(snapshotHolding.instrument)
                            .instrumentToken(snapshotHolding.instrumentToken)
                            .type(snapshotHolding.type)
                            .previousQuantity(snapshotHolding.totalQuantity)
                            .previousPrice(previousPrice)
                            .previousValue(minQuantity * previousPrice)
                            .currentQuantity(currentHolding.totalQuantity)
                            .currentPrice(currentPrice)
                            .currentValue(minQuantity * currentPrice)
                            .minQuantity(minQuantity)
                            .absoluteChange(absoluteChange)
                            .percentageChange(percentageChange)
                            .build();

                    comparisons.add(comparison);
                }
            }
        }

        return comparisons;
    }

    /**
     * Consolidate holdings by trading symbol across all accounts
     */
    private Map<String, ConsolidatedHolding> consolidateHoldingsBySymbol(List<HoldingDetails> holdings) {
        Map<String, ConsolidatedHolding> consolidated = new HashMap<>();

        for (HoldingDetails holding : holdings) {
            String tradingSymbol = holding.getTradingSymbol();

            consolidated.compute(tradingSymbol, (key, existing) -> {
                if (existing == null) {
                    // First occurrence of this symbol
                    ConsolidatedHolding ch = new ConsolidatedHolding();
                    ch.tradingSymbol = tradingSymbol;
                    ch.instrument = holding.getInstrument();
                    ch.instrumentToken = holding.getInstrumentToken();
                    ch.type = holding.getType();
                    ch.totalQuantity = holding.getQuantity() != null ? holding.getQuantity() : 0.0;
                    ch.avgPrice = holding.getLastPrice() != null ? holding.getLastPrice() : 0.0;
                    ch.userIds = holding.getUserId();
                    return ch;
                } else {
                    // Consolidate with existing entry
                    double existingQty = existing.totalQuantity;
                    double newQty = holding.getQuantity() != null ? holding.getQuantity() : 0.0;
                    existing.totalQuantity = existingQty + newQty;

                    // Price should be same across accounts for same symbol, use the first one
                    // If different, it will use the first encountered price
                    if (existing.avgPrice == 0.0 && holding.getLastPrice() != null) {
                        existing.avgPrice = holding.getLastPrice();
                    }

                    // Append userId if different
                    if (!existing.userIds.contains(holding.getUserId())) {
                        existing.userIds = existing.userIds + "," + holding.getUserId();
                    }

                    return existing;
                }
            });
        }

        return consolidated;
    }

    /**
     * Helper class to consolidate holdings across accounts
     */
    private static class ConsolidatedHolding {
        String tradingSymbol;
        String instrument;
        Long instrumentToken;
        String type;
        double totalQuantity;
        double avgPrice;
        String userIds; // Comma-separated list of user IDs holding this instrument
    }

    private void generateAndSendPortfolioReport(List<HoldingComparisonReport> report, String reportType,
                                              DEFAULT_USER_PREFERENCES userPreference, String reportTitle) {
        Map<String, List<HoldingComparisonReport>> reportsByUser = report.stream().collect(Collectors.groupingBy(HoldingComparisonReport::getUserId));
        List<ApplicationUser> applicationUsers = applicationUserRepository.findAll();

        // Get the date range for this report type
        String dateRange = CommonUtil.getReportPeriodDescription(reportType);

        applicationUsers.stream().forEach(applicationUser -> {
            Optional<UserPreferences> userPreferences = userPreferencesRepository.getUserPreferencesByPreferenceAndUserId(userPreference, applicationUser.getId());
            if(userPreferences.isPresent() && userPreferences.get().getValue().equalsIgnoreCase("NO")) {
                log.info("User {} has disabled {} portfolio report. Skipping generation", applicationUser.getUserName(), reportType);
                return;
            }

            String email = applicationUser.getUserName();
            List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(applicationUser, false);

            tradingAccounts.forEach(tradingAccount -> {
                List<HoldingComparisonReport> userReport = reportsByUser.get(tradingAccount.getUserId());
                if(null != userReport) {
                    List<HoldingComparisonReport> ongoingPositionList = userReport.stream()
                            .filter(r -> "ONGOING".equalsIgnoreCase(r.getStatus()))
                            .collect(Collectors.toList());

                    List<HoldingComparisonReport> closedPositionList = userReport.stream()
                            .filter(r -> "CLOSED".equalsIgnoreCase(r.getStatus()))
                            .collect(Collectors.toList());

                    List<HoldingComparisonReport> newPositionList = userReport.stream()
                            .filter(r -> "NEW".equalsIgnoreCase(r.getStatus()))
                            .collect(Collectors.toList());

                    double totalInvestedAmount = ongoingPositionList.stream().mapToDouble(HoldingComparisonReport::getInvestedAmount).sum() +
                                               newPositionList.stream().mapToDouble(HoldingComparisonReport::getInvestedAmount).sum();
                    double totalCurrentAmount = ongoingPositionList.stream().mapToDouble(HoldingComparisonReport::getCurrentValue).sum() +
                                              newPositionList.stream().mapToDouble(HoldingComparisonReport::getCurrentValue).sum();
                    double totalPeriodProfit = ongoingPositionList.stream().mapToDouble(HoldingComparisonReport::getChangeInValueFromLastWeek).sum();
                    double totalPeriodProfitPercentage = totalInvestedAmount != 0 ? (totalPeriodProfit / totalInvestedAmount) * 100 : 0;
                    double netProfit = totalCurrentAmount - totalInvestedAmount;
                    double netProfitPercentage = totalInvestedAmount != 0 ? (netProfit / totalInvestedAmount) * 100 : 0;

                    String subject = "Your " + reportTitle + ": " + tradingAccount.getName() + " ("+tradingAccount.getUserId()+")";
                    String emailBody = buildPortfolioEmailContent(
                            applicationUser.getFullName(),
                            subject,
                            mapToRow(ongoingPositionList),
                            mapToRow(closedPositionList),
                            mapToRow(newPositionList),
                            totalInvestedAmount,
                            totalCurrentAmount,
                            totalPeriodProfit,
                            totalPeriodProfitPercentage,
                            netProfit,
                            netProfitPercentage,
                            reportType,
                            dateRange
                    );

                    ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
                    resendEmailPayload.setTo(new String[]{email});
                    resendEmailPayload.setSubject(subject);
                    resendEmailPayload.setHtml(emailBody);
                    emailService.sendEmail(resendEmailPayload);
                } else {
                    log.warn("User account report not found for {}. Skipping generation", tradingAccount.getUserId());
                }
            });
        });
    }

    private List<HoldingReportRow> mapToRow(List<HoldingComparisonReport> list) {
        return list.stream().map(r -> new HoldingReportRow(
                r.getInstrument(),
                r.getQuantity(),
                r.getInvestedAmount(),
                r.getCurrentValue(),
                r.getChangeInValueFromLastWeek(),
                r.getChangeInValueFromLastWeekPercentage(),
                r.getNetProfitToday(),
                r.getNetProfitTodayPercentage()
        )).collect(Collectors.toList());
    }


    private String normalizeInstrument(String instrument) {
        // Check if this instrument belongs to any cross-exchange mapping
        for (Map.Entry<String, Set<String>> entry : CROSS_EXCHANGE_INSTRUMENT_MAPPING.entrySet()) {
            if (entry.getValue().contains(instrument)) {
                return entry.getKey(); // Return the normalized key
            }
        }
        // If no mapping found, return the instrument as-is
        return instrument;
    }

    private String generateNormalizedKey(HoldingDetails holding) {
        return holding.getUserId() + "_" + normalizeInstrument(holding.getInstrument());
    }

    private List<HoldingComparisonReport> generateHoldingComparisonReport(List<HoldingDetails> latestHoldings,
            List<HoldingDetails> lastWeekHoldings) {

        // Use normalized keys to handle cross-exchange instruments
        Function<HoldingDetails, String> keyGenerator = this::generateNormalizedKey;

        Map<String, HoldingDetails> latestMap = latestHoldings.stream()
                .collect(Collectors.toMap(keyGenerator, h -> h));

        Map<String, HoldingDetails> lastWeekMap = lastWeekHoldings.stream()
                .collect(Collectors.toMap(keyGenerator, h -> h));

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(latestMap.keySet());
        allKeys.addAll(lastWeekMap.keySet());

        List<HoldingComparisonReport> reportList = new ArrayList<>();

        for (String key : allKeys) {
            HoldingDetails latest = latestMap.get(key);
            HoldingDetails lastWeek = lastWeekMap.get(key);
            HoldingComparisonReport report = new HoldingComparisonReport();
            if (latest != null && lastWeek != null) {
                report.setStatus("ONGOING");
                report.setUserId(latest.getUserId());
                // Use normalized instrument name for display
                report.setInstrument(normalizeInstrument(latest.getInstrument()));
                report.setQuantity(latest.getQuantity());
                report.setInvestedAmount(latest.getInvestedAmount());
                report.setCurrentValue(latest.getCurrentValue());
                double change = (latest.getCurrentValue() - lastWeek.getCurrentValue()) - (latest.getInvestedAmount() - lastWeek.getInvestedAmount());
                report.setChangeInValueFromLastWeek(change);
                if (latest.getInvestedAmount() != null && latest.getInvestedAmount() != 0) {
                    report.setChangeInValueFromLastWeekPercentage(change / latest.getInvestedAmount() * 100);
                    report.setNetProfitTodayPercentage(latest.getNetPnl() / latest.getInvestedAmount() * 100);
                } else {
                    report.setChangeInValueFromLastWeekPercentage(0.0);
                    report.setNetProfitTodayPercentage(0.0);
                }
                report.setNetProfitToday(latest.getNetPnl());
            } else if (latest == null && lastWeek != null) {
                report.setStatus("CLOSED");
                report.setUserId(lastWeek.getUserId());
                // Use normalized instrument name for display
                report.setInstrument(normalizeInstrument(lastWeek.getInstrument()));
                report.setInvestedAmount(lastWeek.getInvestedAmount());
                report.setCurrentValue(lastWeek.getCurrentValue());
                report.setNetProfitToday(lastWeek.getNetPnl());
                if (lastWeek.getInvestedAmount() != null && lastWeek.getInvestedAmount() != 0) {
                    report.setNetProfitTodayPercentage(lastWeek.getNetPnl() / lastWeek.getInvestedAmount() * 100);
                } else {
                    report.setNetProfitTodayPercentage(0.0);
                }
            } else if (latest != null) {
                report.setStatus("NEW");
                report.setUserId(latest.getUserId());
                // Use normalized instrument name for display
                report.setInstrument(normalizeInstrument(latest.getInstrument()));
                report.setQuantity(latest.getQuantity());
                report.setInvestedAmount(latest.getInvestedAmount());
                report.setCurrentValue(latest.getCurrentValue());
                report.setNetProfitToday(latest.getNetPnl());
                if (latest.getInvestedAmount() != null && latest.getInvestedAmount() != 0) {
                    report.setNetProfitTodayPercentage(latest.getNetPnl() / latest.getInvestedAmount() * 100);
                } else {
                    report.setNetProfitTodayPercentage(0.0);
                }
            }
            reportList.add(report);
        }
        return reportList;
    }

    private String buildPortfolioEmailContent(String userFullName, String subject,
                                                   List<HoldingReportRow> ongoingPositions,
                                                   List<HoldingReportRow> closedPositions,
                                                   List<HoldingReportRow> newPositions,
                                                   double totalInvestedAmount,
                                                   double totalCurrentAmount,
                                                   double totalPeriodProfit,
                                                   double totalPeriodProfitPercentage,
                                                   double netProfit,
                                                   double netProfitPercentage, String reportType, String dateRange) {
        // Create inner content context
        Context reportContext = new Context();
        reportContext.setVariable("fullName", userFullName);
        reportContext.setVariable("subject", subject);
        reportContext.setVariable("ongoingList", ongoingPositions);
        reportContext.setVariable("closedList", closedPositions);
        reportContext.setVariable("totalInvestedAmount", totalInvestedAmount);
        reportContext.setVariable("totalCurrentAmount", totalCurrentAmount);
        reportContext.setVariable("totalPeriodProfit", totalPeriodProfit);
        reportContext.setVariable("totalPeriodProfitPercentage", totalPeriodProfitPercentage);
        reportContext.setVariable("netProfit", netProfit);
        reportContext.setVariable("netProfitPercentage", netProfitPercentage);
        reportContext.setVariable("newList", newPositions);
        reportContext.setVariable("appUrl", applicationDomain+"/holdings");
        reportContext.setVariable("reportType", reportType);
        reportContext.setVariable("dateRange", dateRange);

        // Process the report template directly (not using base layout)
        String contentHtml =  templateEngine.process("email/portfolio-holdings-report", reportContext);

        // Wrap in base layout
        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);

        return templateEngine.process("email/base-layout", baseContext);
    }

}
