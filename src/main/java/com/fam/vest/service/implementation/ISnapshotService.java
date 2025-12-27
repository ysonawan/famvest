package com.fam.vest.service.implementation;

import com.fam.vest.dto.response.*;
import com.fam.vest.pojo.HistoricalFundsTimeline;
import com.fam.vest.pojo.HistoricalHoldingsTimeline;
import com.fam.vest.pojo.HistoricalMfSipsTimeline;
import com.fam.vest.pojo.HistoricalPositionsTimeline;
import com.fam.vest.service.*;
import com.fam.vest.entity.AccountSnapshot;
import com.fam.vest.entity.HistoricalTimelineValues;
import com.fam.vest.repository.AccountSnapshotRepository;
import com.fam.vest.repository.HistoricalTimelineValuesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ISnapshotService implements SnapshotService {

    private final FundsService fundsService;
    private final HoldingService holdingService;
    private final PositionService positionService;
    private final OrderService orderService;
    private final MutualFundService mutualFundService;
    private final AccountSnapshotRepository accountSnapshotRepository;
    private final HistoricalTimelineValuesRepository historicalTimelineValuesRepository;


    @Autowired
    public ISnapshotService(FundsService fundsService,
                            HoldingService holdingService,
                            PositionService positionService,
                            OrderService orderService,
                            MutualFundService mutualFundService,
                            AccountSnapshotRepository accountSnapshotRepository,
                            HistoricalTimelineValuesRepository historicalTimelineValuesRepository) {
        this.fundsService = fundsService;
        this.holdingService = holdingService;
        this.positionService = positionService;
        this.orderService = orderService;
        this.mutualFundService = mutualFundService;
        this.accountSnapshotRepository = accountSnapshotRepository;
        this.historicalTimelineValuesRepository = historicalTimelineValuesRepository;
    }

    @Override
    public void captureSnapshot() {
        Date currentDate = Calendar.getInstance().getTime();
        log.info("Capturing snapshot for {}", currentDate);
        AccountSnapshot accountSnapshot = new AccountSnapshot();
        accountSnapshot.setSnapshotDate(currentDate);
        accountSnapshot.setCreatedDate(currentDate);
        List<HoldingDetails> holdings = holdingService.getAllHoldings();
        List<PositionDetails> positions = positionService.getAllPositions();
        List<OrderDetails> orders = orderService.getAllOrders();
        List<FundDetails> funds = fundsService.getAllFunds();
        List<MFOrderDetails> mfOrders = mutualFundService.getAllMutualFundOrders();
        List<MFSIPDetails> mfSips = mutualFundService.getAllMutualFundSips();
        accountSnapshotRepository.findAccountSnapshotBySnapshotDate(accountSnapshot.getSnapshotDate()).ifPresentOrElse(snapshot -> {
            snapshot.setLastModifiedDate(currentDate);
            snapshot.setHoldings(holdings);
            snapshot.setPositions(positions);
            snapshot.setOrders(orders);
            snapshot.setFunds(funds);
            snapshot.setMfOrders(mfOrders);
            snapshot.setMfSips(mfSips);
            log.info("Updating accountSnapshot data for date: {}", accountSnapshot.getSnapshotDate());
            accountSnapshotRepository.save(snapshot);
        }, () -> {
            accountSnapshot.setCreatedDate(currentDate);
            accountSnapshot.setLastModifiedDate(currentDate);
            accountSnapshot.setHoldings(holdings);
            accountSnapshot.setPositions(positions);
            accountSnapshot.setOrders(orders);
            accountSnapshot.setFunds(funds);
            accountSnapshot.setMfOrders(mfOrders);
            accountSnapshot.setMfSips(mfSips);
            log.info("Saving accountSnapshot data for date: {}", accountSnapshot.getSnapshotDate());
            accountSnapshotRepository.save(accountSnapshot);
        });
        log.info("Account snapshot saved for date: {}", accountSnapshot.getSnapshotDate());
        this.captureHistoricalTimelineValues();
    }

    private void captureHistoricalTimelineValues() {
        log.info("Capturing historical timeline values for account snapshots");
        Date currentDate = Calendar.getInstance().getTime();
        HistoricalTimelineValues latestHistoricalTimelineValues = historicalTimelineValuesRepository.findLatestHistoricalTimelineValues(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        List<AccountSnapshot> accountSnapshots = new ArrayList<>();
        if(null == latestHistoricalTimelineValues) {
            log.info("Historical timeline values not found. Capturing new values all historical snapshot data");
            accountSnapshots = accountSnapshotRepository.findAll();
        } else {
            log.info("Historical timeline values found till: {}. Capturing new values for the remaining historical snapshot data", latestHistoricalTimelineValues.getDate());
            accountSnapshots = accountSnapshotRepository.findBySnapshotDateGreaterThanEqual(latestHistoricalTimelineValues.getDate());
        }
        log.info("Processing historical timeline values for: {} account snapshots", accountSnapshots.size());
        accountSnapshots.forEach(accountSnapshot -> {
            HistoricalTimelineValues historicalTimelineValues = historicalTimelineValuesRepository.findHistoricalTimelineValuesByDate(accountSnapshot.getSnapshotDate()).orElse(new HistoricalTimelineValues());
            historicalTimelineValues.setDate(accountSnapshot.getSnapshotDate());
            historicalTimelineValues.setCreatedDate(currentDate);
            historicalTimelineValues.setLastModifiedDate(currentDate);

            log.info("Processing historical holdings timeline values {}", accountSnapshot.getSnapshotDate());
            List<HoldingDetails> holdings = accountSnapshot.getHoldings();
            if(null != holdings) {
                List<HistoricalHoldingsTimeline> historicalHoldingsTimeline = this.summarizeHoldings(holdings, accountSnapshot.getSnapshotDate());
                historicalTimelineValues.setHistoricalHoldingsTimelines(historicalHoldingsTimeline);
            } else {
                log.warn("Historical holdings not found for account snapshot {}", accountSnapshot.getSnapshotDate());
            }
            log.info("Processing mf sips timeline values {}", accountSnapshot.getSnapshotDate());
            List<MFSIPDetails> mfSips = accountSnapshot.getMfSips();
            if(null != mfSips) {
                List<HistoricalMfSipsTimeline> historicalMfSipsTimelines = this.summarizeMfSips(mfSips, accountSnapshot.getSnapshotDate());
                historicalTimelineValues.setHistoricalMfSipsTimelines(historicalMfSipsTimelines);
            } else {
                log.warn("MF sips not found for account snapshot {}", accountSnapshot.getSnapshotDate());
            }
            log.info("Processing funds timeline values {}", accountSnapshot.getSnapshotDate());
            List<FundDetails> funds = accountSnapshot.getFunds();
            if(null != funds) {
                List<HistoricalFundsTimeline> historicalFundsTimelines = this.summarizeFunds(funds, accountSnapshot.getSnapshotDate());
                historicalTimelineValues.setHistoricalFundsTimelines(historicalFundsTimelines);
            } else {
                log.warn("Funds not found for account snapshot {}", accountSnapshot.getSnapshotDate());
            }
            log.info("Processing positions timeline values {}", accountSnapshot.getSnapshotDate());
            List<PositionDetails> positions = accountSnapshot.getPositions();
            if(null != positions) {
                List<HistoricalPositionsTimeline> historicalPositionsTimelines = this.summarizePositions(positions, accountSnapshot.getSnapshotDate());
                historicalTimelineValues.setHistoricalPositionsTimelines(historicalPositionsTimelines);
            } else {
                log.warn("Positions not found for account snapshot {}", accountSnapshot.getSnapshotDate());
            }
            historicalTimelineValuesRepository.save(historicalTimelineValues);
        });
        log.info("Capturing historical timeline values for account snapshots completed");
    }

    private List<HistoricalPositionsTimeline> summarizePositions(List<PositionDetails> positions, Date date) {
        List<HistoricalPositionsTimeline> summaries = new ArrayList<>();
        try {
            // Group positions by userId
            Map<String, List<PositionDetails>> positionsByUser = positions.stream()
                    .collect(Collectors.groupingBy(PositionDetails::getUserId));

            for (Map.Entry<String, List<PositionDetails>> entry : positionsByUser.entrySet()) {
                String userId = entry.getKey();
                List<PositionDetails> positionDetails = entry.getValue();

                HistoricalPositionsTimeline historicalPositionsTimeline = new HistoricalPositionsTimeline();
                historicalPositionsTimeline.setDate(date);
                historicalPositionsTimeline.setUserId(userId);
                historicalPositionsTimeline.setOpenDerivativePositions(positionDetails.stream().
                        filter(position -> position.getPosition().netQuantity !=0 && (position.getPosition().exchange.equalsIgnoreCase("BFO") ||
                                position.getPosition().exchange.equalsIgnoreCase("NFO"))).count());

                historicalPositionsTimeline.setExitedDerivativePositions(positionDetails.stream().
                        filter(position -> position.getPosition().netQuantity == 0 && (position.getPosition().exchange.equalsIgnoreCase("BFO") ||
                                position.getPosition().exchange.equalsIgnoreCase("NFO"))).count());

                historicalPositionsTimeline.setTotalEodPnl(positionDetails.stream().
                        filter(position -> (position.getPosition().exchange.equalsIgnoreCase("BFO") ||
                                position.getPosition().exchange.equalsIgnoreCase("NFO"))).
                        mapToDouble(position -> position.getPosition().pnl).sum());

                summaries.add(historicalPositionsTimeline);
            }
        } catch (Exception exception) {
            log.error("Error occurred while calculating summary for Positions", exception);
        }
        return summaries;
    }

    private List<HistoricalFundsTimeline> summarizeFunds(List<FundDetails> funds, Date date) {
        List<HistoricalFundsTimeline> summaries = new ArrayList<>();
        try {
            // Group funds by userId
            Map<String, List<FundDetails>> fundsByUser = funds.stream()
                    .collect(Collectors.groupingBy(FundDetails::getUserId));

            for (Map.Entry<String, List<FundDetails>> entry : fundsByUser.entrySet()) {
                String userId = entry.getKey();
                List<FundDetails> fundDetails = entry.getValue();

                HistoricalFundsTimeline historicalFundsTimeline = new HistoricalFundsTimeline();
                historicalFundsTimeline.setDate(date);
                historicalFundsTimeline.setUserId(userId);
                historicalFundsTimeline.setAvailableCash(fundDetails.stream().
                        mapToDouble(fund -> Double.valueOf(fund.getMargin().available.liveBalance)).sum());
                historicalFundsTimeline.setAvailableMargin(fundDetails.stream().
                        mapToDouble(fund -> Double.valueOf(fund.getMargin().net)).sum());
                historicalFundsTimeline.setUsedMargin(fundDetails.stream().
                        mapToDouble(fund -> Double.valueOf(fund.getMargin().utilised.debits)).sum());
                historicalFundsTimeline.setTotalCollateral(fundDetails.stream().
                        mapToDouble(fund -> Double.valueOf(fund.getMargin().available.collateral)).sum());

                summaries.add(historicalFundsTimeline);
            }
        } catch (Exception exception) {
            log.error("Error occurred while calculating summary for funds", exception);
        }
        return summaries;
    }

    private List<HistoricalMfSipsTimeline> summarizeMfSips(List<MFSIPDetails> mfSips, Date date) {
        List<HistoricalMfSipsTimeline> summaries = new ArrayList<>();
        try {
            // Group mf sips by userId
            Map<String, List<MFSIPDetails>> mfSipsByUser = mfSips.stream()
                    .collect(Collectors.groupingBy(MFSIPDetails::getUserId));

            for (Map.Entry<String, List<MFSIPDetails>> entry : mfSipsByUser.entrySet()) {
                String userId = entry.getKey();
                List<MFSIPDetails> sips = entry.getValue();

                HistoricalMfSipsTimeline historicalMfSipsTimeline = new HistoricalMfSipsTimeline();
                historicalMfSipsTimeline.setDate(date);
                historicalMfSipsTimeline.setUserId(userId);
                historicalMfSipsTimeline.setSipAmount(sips.stream().
                        filter(sip -> sip.getMfSip().status.equalsIgnoreCase("ACTIVE")).
                        mapToDouble(sip -> sip.getMfSip().instalmentAmount).sum());

                historicalMfSipsTimeline.setActiveSips(sips.stream().
                        filter(sip -> sip.getMfSip().status.equalsIgnoreCase("ACTIVE")).count());

                historicalMfSipsTimeline.setPausedSips(sips.stream().
                        filter(sip -> sip.getMfSip().status.equalsIgnoreCase("PAUSED")).count());

                summaries.add(historicalMfSipsTimeline);
            }
        } catch (Exception exception) {
            log.error("Error occurred while calculating summary for mf sips", exception);
        }
        return summaries;
    }

    private List<HistoricalHoldingsTimeline> summarizeHoldings(List<HoldingDetails> holdings, Date date) {
        List<HistoricalHoldingsTimeline> summaries = new ArrayList<>();
        try {
            // Group holdings by userId
            Map<String, List<HoldingDetails>> holdingsByUser = holdings.stream()
                    .collect(Collectors.groupingBy(HoldingDetails::getUserId));

            for (Map.Entry<String, List<HoldingDetails>> entry : holdingsByUser.entrySet()) {
                String userId = entry.getKey();
                List<HoldingDetails> userHoldings = entry.getValue();

                HistoricalHoldingsTimeline historicalHoldingsTimeline = new HistoricalHoldingsTimeline();
                historicalHoldingsTimeline.setDate(date);
                historicalHoldingsTimeline.setUserId(userId);
                historicalHoldingsTimeline.setInvestedAmount(userHoldings.stream()
                        .filter(h -> h.getInvestedAmount() != null)
                        .mapToDouble(HoldingDetails::getInvestedAmount).sum());

                historicalHoldingsTimeline.setCurrentValue(userHoldings.stream()
                        .filter(h -> h.getCurrentValue() != null)
                        .mapToDouble(HoldingDetails::getCurrentValue).sum());

                historicalHoldingsTimeline.setDayPnl(userHoldings.stream()
                        .filter(h -> h.getDayPnl() != null)
                        .mapToDouble(HoldingDetails::getDayPnl).sum());

                historicalHoldingsTimeline.setNetPnl(userHoldings.stream()
                        .filter(h -> h.getNetPnl() != null)
                        .mapToDouble(HoldingDetails::getNetPnl).sum());

                double investedAmount = historicalHoldingsTimeline.getInvestedAmount();
                double netPnl = historicalHoldingsTimeline.getNetPnl();
                if (investedAmount != 0.0) {
                    historicalHoldingsTimeline.setNetChangePercentage(netPnl * 100 / investedAmount);
                } else {
                    historicalHoldingsTimeline.setNetChangePercentage(0.0);
                }

                double currentValue = historicalHoldingsTimeline.getCurrentValue();
                double dayPnl = historicalHoldingsTimeline.getDayPnl();
                if (currentValue != 0.0) {
                    historicalHoldingsTimeline.setDayChangePercentage(dayPnl * 100 / currentValue);
                } else {
                    historicalHoldingsTimeline.setDayChangePercentage(0.0);
                }
                summaries.add(historicalHoldingsTimeline);
            }
        } catch (Exception exception) {
            log.error("Error occurred while calculating summary for holdings", exception);
        }
        return summaries;
    }

}
