package com.fam.vest.service.implementation;

import com.fam.vest.cache.MutualFundNavCacheService;
import com.fam.vest.config.KiteConnector;
import com.fam.vest.dto.request.SIPRequest;
import com.fam.vest.dto.response.HoldingDetails;
import com.fam.vest.dto.response.MFOrderDetails;
import com.fam.vest.dto.response.MFSIPDetails;
import com.fam.vest.entity.AccountSnapshot;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.entity.UserPreferences;
import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.pojo.MutualFundNav;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.repository.AccountSnapshotRepository;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.UserPreferencesRepository;
import com.fam.vest.service.EmailService;
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
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMutualFundService implements MutualFundService {

    private final TradingAccountService tradingAccountService;
    private final KiteConnector kiteConnector;
    private final MutualFundNavCacheService mutualFundNavCacheService;
    private final AccountSnapshotRepository accountSnapshotRepository;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final ApplicationUserRepository applicationUserRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    @Value("${fam.vest.app.domain}")
    private String applicationDomain;

    @Override
    public List<HoldingDetails> getMutualFundHoldings(UserDetails userDetails, Optional<String> tradingAccountId) {
        List<TradingAccount> tradingAccounts = this.getApplicableTradingAccounts(userDetails, tradingAccountId);
        List<HoldingDetails> holdingDetails = new ArrayList<>();
        AtomicLong sequenceNumber = new AtomicLong(1);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                this.getMutualFundHoldings(tradingAccount.getUserId(), kiteConnect, holdingDetails, sequenceNumber);
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting mf holdings for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return holdingDetails;
    }

    @Override
    public void getMutualFundHoldings(String userId, KiteConnect kiteConnect, List<HoldingDetails> holdingDetails, AtomicLong sequenceNumber)
            throws KiteException, IOException {
        kiteConnect.getMFHoldings().forEach(holding -> {
            if(StringUtils.hasText(holding.fund)) {
                HoldingDetails holdingDetail = new HoldingDetails();
                holdingDetail.setType("Mutual Funds");
                holdingDetail.setUserId(userId);
                holdingDetail.setInstrument(holding.fund);
                holdingDetail.setTradingSymbol(holding.tradingsymbol);
                holdingDetail.setQuantity(holding.quantity);
                holdingDetail.setAveragePrice(holding.averagePrice);
                holdingDetail.setLastPrice(holding.lastPrice);
                holdingDetail.setInvestedAmount(holdingDetail.getQuantity() * holdingDetail.getAveragePrice());
                try {
                    MutualFundNav mfNavResponse = mutualFundNavCacheService.getMutualFundNav(holding.tradingsymbol);
                    if (mfNavResponse == null) {
                        log.warn("NAV not found for mutual fund: {}", holding.tradingsymbol);
                    } else {
                        if (mfNavResponse.getNav() != null && mfNavResponse.getLastNav() != null) {
                            holdingDetail.setLastPrice(mfNavResponse.getNav().getNav());
                            holdingDetail.setCurrentValue(holdingDetail.getQuantity() * holdingDetail.getLastPrice());
                            holdingDetail.setDayBeforeLastPrice(mfNavResponse.getLastNav().getNav());
                            holdingDetail.setDayChange(holdingDetail.getLastPrice() - holdingDetail.getDayBeforeLastPrice());
                            holdingDetail.setDayChangePercentage(holdingDetail.getDayChange() * 100 / holdingDetail.getDayBeforeLastPrice());
                            holdingDetail.setDayPnl(holdingDetail.getDayChangePercentage() * holdingDetail.getCurrentValue() / 100);
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception occurred while setting mutual fund holdings nav: {}", holding.tradingsymbol, e);
                }
                holdingDetail.setCurrentValue(holdingDetail.getQuantity() * holdingDetail.getLastPrice());
                holdingDetail.setNetPnl(holdingDetail.getCurrentValue() - holdingDetail.getInvestedAmount());
                holdingDetail.setNetChangePercentage((holdingDetail.getNetPnl() * 100.0) / holdingDetail.getInvestedAmount());
                holdingDetail.setSequenceNumber(sequenceNumber.getAndIncrement());
                holdingDetail.setPnl(holding.pnl);
                holdingDetail.setCollateralQuantity(String.valueOf(holding.pledgedQuantity));
                holdingDetails.add(holdingDetail);
            } else {
                log.warn("Fund name not found for mutual fund : {}. Not adding to the mf holdings list", holding.tradingsymbol);
            }
        });
    }

    @Override
    public List<MFOrderDetails> getAllMutualFundOrders() {
        return this.getConsolidatedMutualFundOrders(null, Optional.ofNullable(null));
    }

    @Override
    public List<MFOrderDetails> getConsolidatedMutualFundOrders(UserDetails userDetails, Optional<String> tradingAccountId) {
        List<TradingAccount> tradingAccounts = this.getApplicableTradingAccounts(userDetails, tradingAccountId);
        AccountSnapshot latest = accountSnapshotRepository.findLatestSnapshot(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        List<MFOrderDetails> snapshotMfOrderDetails = new ArrayList<>();
        List<MFOrderDetails> consolidatedMfOrderDetails = null;
        if(null != latest) {
            Date startOfLastMonthDate = CommonUtil.getStartOfLastMonthDate();
            List<String> tradingAccountIds = tradingAccounts.stream().map(TradingAccount::getUserId).toList();
            snapshotMfOrderDetails = latest.getMfOrders().stream().filter(order -> tradingAccountIds.contains(order.getUserId())).toList();
            snapshotMfOrderDetails = snapshotMfOrderDetails.stream().filter(order -> order.getMfOrder().orderTimestamp.after(startOfLastMonthDate)).toList();
        } else {
            log.warn("No latest account snapshot found while fetching mf orders");
        }
        List<MFOrderDetails> liveMfOrderDetails = this.getLiveMutualFundOrders(tradingAccounts);
        Map<String, MFOrderDetails> merged = new LinkedHashMap<>();
        // Add snapshot orders
        snapshotMfOrderDetails.forEach(order -> {
            if(null != order && null != order.getMfOrder()) {
                merged.put(order.getMfOrder().orderId, order);
            }
        });
        // Overwrite with live orders
        liveMfOrderDetails.forEach(order -> {
            if(null != order && null != order.getMfOrder()) {
                merged.put(order.getMfOrder().orderId, order);
            }
        });
        consolidatedMfOrderDetails = new ArrayList<>(merged.values());
        //sort using order timestamp in descending order
        consolidatedMfOrderDetails.sort((o1, o2) -> o2.getMfOrder().orderTimestamp.compareTo(o1.getMfOrder().orderTimestamp));
        return consolidatedMfOrderDetails;
    }

    private List<MFOrderDetails> getLiveMutualFundOrders(List<TradingAccount> tradingAccounts) {
        List<MFOrderDetails> mfOrderDetails = new ArrayList<>();
        AtomicLong sequenceNumber = new AtomicLong(1);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                this.getMutualFundOrders(tradingAccount.getUserId(), kiteConnect, mfOrderDetails, sequenceNumber);
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting mf orders for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return mfOrderDetails;
    }

    private void getMutualFundOrders(String userId, KiteConnect kiteConnect, List<MFOrderDetails> mfOrderDetails, AtomicLong sequenceNumber)
            throws KiteException, IOException {
        kiteConnect.getMFOrders().forEach(mfOrder -> {
            MFOrderDetails mfOrderDetail = new MFOrderDetails();
            mfOrderDetail.setUserId(userId);
            mfOrderDetail.setSequenceNumber(sequenceNumber.getAndIncrement());
            mfOrderDetail.setMfOrder(mfOrder);
            try {
                MutualFundNav mfNavResponse = mutualFundNavCacheService.getMutualFundNav(mfOrder.tradingsymbol);
                if (mfNavResponse == null) {
                    log.warn("NAV not found for mutual fund: {}", mfOrder.tradingsymbol);
                } else {
                    if (mfNavResponse.getNav() != null && mfNavResponse.getLastNav() != null) {
                        mfOrderDetail.setLastPrice(mfNavResponse.getNav().getNav());
                        mfOrderDetail.setDayBeforeLastPrice(mfNavResponse.getLastNav().getNav());
                        mfOrderDetail.setDayChange(mfOrderDetail.getLastPrice() - mfOrderDetail.getDayBeforeLastPrice());
                        mfOrderDetail.setDayChangePercentage(mfOrderDetail.getDayChange() * 100 / mfOrderDetail.getDayBeforeLastPrice());
                    }
                }
            } catch (Exception e) {
                log.error("Exception occurred while setting mutual fund orders nav: {}", mfOrder.tradingsymbol, e);
            }
            mfOrderDetails.add(mfOrderDetail);
        });
    }

    @Override
    public List<MFSIPDetails> updateSip(String sipId, String tradingAccountId, UserDetails userDetails, SIPRequest sipRequest) {
        List<MFSIPDetails> mfSipDetails;
        try {
            TradingAccount tradingAccount = tradingAccountService.getTradingAccount(userDetails, tradingAccountId);
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            kiteConnect.modifyMFSIP(sipRequest.getSipId(), sipRequest.getDay(), sipRequest.getInstalments(),
                    sipRequest.getAmount(), sipRequest.getStatus(), sipId);
            mfSipDetails = this.getMutualFundSips(userDetails, Optional.ofNullable(null));
        } catch (RequestTokenMissingException e) {
            String errorMessage = "Request token missing for trading user: "+tradingAccountId+". SIP modification skipped";
            log.error(errorMessage);
            throw new InternalException(errorMessage);
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while modifying sip for sipId: {}, userId: {}. Error: {}", sipId, tradingAccountId, errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return mfSipDetails;
    }

    @Override
    public List<MFSIPDetails> getAllMutualFundSips() {
        return this.getMutualFundSips(null, Optional.ofNullable(null));
    }

    @Override
    public List<MFSIPDetails> getMutualFundSips(UserDetails userDetails, Optional<String> tradingAccountId) {
        List<TradingAccount> tradingAccounts = this.getApplicableTradingAccounts(userDetails, tradingAccountId);
        List<MFSIPDetails> mfSipDetails = new ArrayList<>();
        AtomicLong sequenceNumber = new AtomicLong(1);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                this.getMutualFundSips(tradingAccount.getUserId(), kiteConnect, mfSipDetails, sequenceNumber);
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting mf sips for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return mfSipDetails;
    }

    private void getMutualFundSips(String userId, KiteConnect kiteConnect, List<MFSIPDetails> mfSipDetails, AtomicLong sequenceNumber)
            throws KiteException, IOException {
        kiteConnect.getMFSIPs().forEach(mfsip -> {
            MFSIPDetails mfSipDetail = new MFSIPDetails();
            mfSipDetail.setUserId(userId);
            mfSipDetail.setSequenceNumber(sequenceNumber.getAndIncrement());
            mfSipDetail.setMfSip(mfsip);
            try {
                MutualFundNav mfNavResponse = mutualFundNavCacheService.getMutualFundNav(mfsip.tradingsymbol);
                if (mfNavResponse == null) {
                    log.warn("NAV not found for mutual fund: {}", mfsip.tradingsymbol);
                } else {
                    if (mfNavResponse.getNav() != null && mfNavResponse.getLastNav() != null) {
                        mfSipDetail.setLastPrice(mfNavResponse.getNav().getNav());
                        mfSipDetail.setDayBeforeLastPrice(mfNavResponse.getLastNav().getNav());
                        mfSipDetail.setDayChange(mfSipDetail.getLastPrice() - mfSipDetail.getDayBeforeLastPrice());
                        mfSipDetail.setDayChangePercentage(mfSipDetail.getDayChange() * 100 / mfSipDetail.getDayBeforeLastPrice());
                    }
                }
            } catch (Exception e) {
                log.error("Exception occurred while setting mutual fund sips nav: {}", mfsip.tradingsymbol, e);
            }
            mfSipDetails.add(mfSipDetail);
        });
    }

    private List<TradingAccount> getApplicableTradingAccounts(UserDetails userDetails, Optional<String> tradingAccountId) {
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
        return tradingAccounts;
    }

    @Override
    public void generateAndNotifyMonthlySipReport() {
        log.info("Monthly sip report generation started");
        AccountSnapshot latestAccountSnapshot = accountSnapshotRepository.findLatestSnapshot(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        Date startOfLastMonthDate = CommonUtil.getStartOfLastMonthDate();
        Date endOfLastMonthDate = CommonUtil.getEndOfLastMonthDate();

        List<MFSIPDetails> latestMfSips = latestAccountSnapshot != null ? latestAccountSnapshot.getMfSips() : new ArrayList<>();

        List<MFOrderDetails> mfOrderDetails = new ArrayList<>();
        AccountSnapshot endOfLastMonthAccountSnapshot = accountSnapshotRepository.findAccountSnapshotBySnapshotDate(endOfLastMonthDate).orElse(null);
        if (endOfLastMonthAccountSnapshot != null) {
            mfOrderDetails = endOfLastMonthAccountSnapshot.getMfOrders().stream()
                    .filter(order -> order.getMfOrder().orderTimestamp.after(startOfLastMonthDate)).toList();
        }
        List<ApplicationUser> applicationUsers = applicationUserRepository.findAll();
        List<MFOrderDetails> finalMfOrderDetails = mfOrderDetails;
        applicationUsers.stream().forEach(applicationUser -> {
            Optional<UserPreferences> userPreferences = userPreferencesRepository.getUserPreferencesByPreferenceAndUserId(DEFAULT_USER_PREFERENCES.MONTHLY_SIP_REPORT, applicationUser.getId());
            if(userPreferences.isPresent() && userPreferences.get().getValue().equalsIgnoreCase("NO")) {
                log.info("User {} has disabled monthly sip report. Skipping generation", applicationUser.getUserName());
                return;
            }
            String email = applicationUser.getUserName();
            List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(applicationUser, false);
            tradingAccounts.forEach(tradingAccount -> {
                String subject = "Your Monthly Mutual Fund SIP & Order Report: " + tradingAccount.getName() + " ("+tradingAccount.getUserId()+")";
                List<MFSIPDetails> mfSips = latestMfSips.stream().filter(mfSip -> mfSip.getUserId().equals(tradingAccount.getUserId())).toList();
                List<MFOrderDetails> mfOrders = new ArrayList<>(finalMfOrderDetails.stream().filter(mfOrder -> mfOrder.getUserId().equals(tradingAccount.getUserId())).toList());
                //sort using order timestamp in asc order
                mfOrders.sort(Comparator.comparing(o -> o.getMfOrder().orderTimestamp));

                if(mfSips.isEmpty() && mfOrders.isEmpty()) {
                    log.info("Mutual fund sips and orders are empty for trading account: {}. Report skipped", tradingAccount.getUserId());
                } else {
                    double totalSipAmount = mfSips.stream().filter(sip -> "ACTIVE".equalsIgnoreCase(sip.getMfSip().status))
                            .mapToDouble(mfSip -> mfSip.getMfSip().instalmentAmount)
                            .sum();

                    long activeSipCount = mfSips.stream()
                            .filter(sip -> "ACTIVE".equalsIgnoreCase(sip.getMfSip().status))
                            .count();

                    long pausedSipCount = mfSips.stream()
                            .filter(sip -> "PAUSED".equalsIgnoreCase(sip.getMfSip().status))
                            .count();
                    String emailBody = buildMonthlySipReportEmailContent(
                            applicationUser.getFullName(),
                            subject,
                            mfSips,
                            mfOrders,
                            totalSipAmount,
                            activeSipCount,
                            pausedSipCount
                    );
                    ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
                    resendEmailPayload.setTo(new String[]{email});
                    resendEmailPayload.setSubject(subject);
                    resendEmailPayload.setHtml(emailBody);
                    emailService.sendEmail(resendEmailPayload);
                }
            });
        });
        log.info("Monthly sip report generation completed");
    }


    private String buildMonthlySipReportEmailContent(String userFullName, String subject,
                                                     List<MFSIPDetails> sipList,
                                                     List<MFOrderDetails> orderList, double totalSipAmount, long activeSipCount, long pausedSipCount) {
        Context reportContext = new Context();
        reportContext.setVariable("fullName", userFullName);
        reportContext.setVariable("sipList", sipList);
        reportContext.setVariable("orderList", orderList);
        reportContext.setVariable("appUrl", applicationDomain + "/mf");
        reportContext.setVariable("totalSipAmount", totalSipAmount);
        reportContext.setVariable("activeSipCount", activeSipCount);
        reportContext.setVariable("pausedSipCount", pausedSipCount);

        String contentHtml = templateEngine.process("email/monthly-mf-sip-order-report", reportContext);

        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);

        return templateEngine.process("email/base-layout", baseContext);
    }

}
