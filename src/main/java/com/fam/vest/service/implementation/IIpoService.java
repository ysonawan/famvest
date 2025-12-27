package com.fam.vest.service.implementation;

import com.fam.vest.cache.IpoCacheService;
import com.fam.vest.config.KiteInternalAPIConnector;
import com.fam.vest.dto.request.IpoBidRequest;
import com.fam.vest.dto.response.*;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.Ipo;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.entity.UserPreferences;
import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import com.fam.vest.exception.EncTokenExpiredException;
import com.fam.vest.pojo.IpoDetails;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.IpoRepository;
import com.fam.vest.repository.UserPreferencesRepository;
import com.fam.vest.scraper.ZerodhaIpoScraper;
import com.fam.vest.service.EmailService;
import com.fam.vest.service.IpoService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.KITE_INTERNAL_API;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
@Service
public class IIpoService implements IpoService {

    private final IpoRepository ipoRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final ApplicationUserRepository applicationUserRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final IpoCacheService ipoCacheService;
    private final KiteInternalAPIConnector kiteInternalAPIConnector;
    private final TradingAccountService tradingAccountService;

    @Override
    public List<IpoData> getIpos(Optional<String> status) {
        List<IpoData> ipoDataList = List.of();
        IpoResponse ipoResponse = ipoCacheService.getIpos();
        if(null != ipoResponse) {
            ipoDataList = ipoResponse.getData();
            if(status.isPresent()) {
                ipoDataList = ipoDataList.stream().filter(ipoData -> ipoData.getStatus().equalsIgnoreCase(status.get().toUpperCase())).toList();
            }
        }
        return ipoDataList;
    }

    @Override
    public List<IpoApplication> getIpoApplications(Optional<String> tradingAccountId, UserDetails userDetails) {
        List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        if (tradingAccountId.isPresent()) {
            tradingAccounts = tradingAccounts.stream()
                    .filter(tradingAccount -> tradingAccount.getUserId().equals(tradingAccountId.get()))
                    .toList();
        }
        List<IpoApplication> ipoApplications = new ArrayList<>();
        for (TradingAccount tradingAccount : tradingAccounts) {
            IpoApplicationResponse ipoApplicationResponse = this.getIpoApplicationsFromKiteAPI(tradingAccount);
            if (ipoApplicationResponse != null && ipoApplicationResponse.getData() != null) {
                ipoApplications.addAll(ipoApplicationResponse.getData());
            }
        }
        return ipoApplications;
    }

    private Optional<TradingAccount> getTradingAccountById(String tradingAccountId, UserDetails userDetails) {
        List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, false);
        return tradingAccounts.stream().filter(ta -> ta.getUserId().equals(tradingAccountId)).findFirst();
    }

    @Override
    public VpaData getVPA(String tradingAccountId, UserDetails userDetails) {
        Optional<TradingAccount> tradingAccount = this.getTradingAccountById(tradingAccountId, userDetails);
        if (tradingAccount.isEmpty()) {
            log.warn("Trading account with id: {} not found for user: {}", tradingAccountId, userDetails.getUsername());
            return new VpaData();
        }
        return this.getVPAFromKiteAPI(tradingAccount.get()).getData();
    }

    @Override
    public GeneralKiteData submitIpoApplication(String tradingAccountId, UserDetails userDetails, IpoBidRequest ipoBidRequest) {
        Optional<TradingAccount> tradingAccount = this.getTradingAccountById(tradingAccountId, userDetails);
        if (tradingAccount.isEmpty()) {
            log.warn("Trading account with id: {} not found for user: {}", tradingAccountId, userDetails.getUsername());
            return new GeneralKiteData();
        }
        return this.submitIpoApplication(tradingAccount.get(), ipoBidRequest).getData();
    }

    @Override
    public GeneralKiteData cancelIpoApplication(String tradingAccountId, UserDetails userDetails, String applicationId) {
        Optional<TradingAccount> tradingAccount = this.getTradingAccountById(tradingAccountId, userDetails);
        if (tradingAccount.isEmpty()) {
            log.warn("Trading account with id: {} not found for user: {}", tradingAccountId, userDetails.getUsername());
            return new GeneralKiteData();
        }
        return this.cancelIpoApplication(tradingAccount.get(), applicationId).getData();
    }

    /**
     * Generic helper method to execute Kite API calls with automatic retry on EncTokenExpired
     * @param operation Functional interface that executes the API call
     * @param defaultResponse Default response object to return on failure
     * @param operationName Name of the operation for logging purposes
     * @param userId User ID for logging purposes
     * @return Response from the API call or default response on failure
     */
    private <T> T executeWithRetry(KiteApiOperation<T> operation, T defaultResponse, String operationName, String userId) {
        try {
            return operation.execute();
        } catch (EncTokenExpiredException encTokenExpiredException) {
            log.info("encToken expired for account: {}, retrying...", userId);
            try {
                return operation.execute();
            } catch (EncTokenExpiredException ex) {
                log.error("Failed to {} after refreshing encToken for account: {}", operationName, userId);
                return defaultResponse;
            }
        }
    }

    private IpoApplicationResponse getIpoApplicationsFromKiteAPI(TradingAccount tradingAccount) {
        log.debug("Getting ipo applications from Kite Internal API...");
        return executeWithRetry(
                () -> kiteInternalAPIConnector.get(KITE_INTERNAL_API.GET_IPO_APPLICATIONS, tradingAccount, IpoApplicationResponse.class),
                new IpoApplicationResponse(),
                "retrieve IPOs",
                tradingAccount.getUserId()
        );
    }

    private VpaResponse getVPAFromKiteAPI(TradingAccount tradingAccount) {
        log.debug("Getting ipo vpa from Kite Internal API...");
        return executeWithRetry(
                () -> kiteInternalAPIConnector.get(KITE_INTERNAL_API.GET_VPA, tradingAccount, VpaResponse.class),
                new VpaResponse(),
                "retrieve VPA",
                tradingAccount.getUserId()
        );
    }

    private GeneralKiteResponse submitIpoApplication(TradingAccount tradingAccount, IpoBidRequest ipoBidRequest) {
        log.debug("Submitting ipo application from Kite Internal API...");
        return executeWithRetry(
                () -> kiteInternalAPIConnector.post(KITE_INTERNAL_API.SUBMIT_IPO_APPLICATION, tradingAccount, GeneralKiteResponse.class, ipoBidRequest),
                new GeneralKiteResponse(),
                "submit IPO",
                tradingAccount.getUserId()
        );
    }

    private GeneralKiteResponse cancelIpoApplication(TradingAccount tradingAccount, String applicationId) {
        log.debug("Cancelling ipo application from Kite Internal API...");
        return executeWithRetry(
                () -> kiteInternalAPIConnector.delete(KITE_INTERNAL_API.CANCEL_IPO_APPLICATION.replace("{id}", applicationId), tradingAccount, GeneralKiteResponse.class),
                new GeneralKiteResponse(),
                "cancel IPO",
                tradingAccount.getUserId()
        );
    }

    @Override
    public void refreshIposFromKiteInternalApi() {
        ipoCacheService.refreshIposFromKiteInternalApi();
    }

    @Override
    public void retrieveAndSaveIpos() {
        log.info("Retrieving and saving IPOs...");
        ZerodhaIpoScraper ipoScraper = new ZerodhaIpoScraper();
        List<IpoDetails> ipoDetails = ipoScraper.loadAllIpos();
        if(!ipoDetails.isEmpty()) {
            log.info("Clearing existing IPOs from the repository.");
            ipoRepository.deleteAll();
            log.info("Saving new IPOs to the repository.");
            Calendar calendar = Calendar.getInstance();
            ipoDetails.forEach(ipo -> {
                Ipo ipoEntity = new Ipo();
                ipoEntity.setSymbol(ipo.getSymbol());
                ipoEntity.setType(ipo.getType());
                ipoEntity.setName(ipo.getName());
                ipoEntity.setDetailsUrl(ipo.getDetailsUrl());
                ipoEntity.setLogoUrl(ipo.getLogoUrl());
                ipoEntity.setStartDate(ipo.getStartDate());
                ipoEntity.setEndDate(ipo.getEndDate());
                ipoEntity.setListingDate(ipo.getListingDate());
                ipoEntity.setPriceRange(ipo.getPriceRange());
                ipoEntity.setStatus(ipo.getStatus());
                ipoEntity.setCreatedDate(calendar.getTime());
                ipoEntity.setLastModifiedDate(calendar.getTime());
                log.debug("Saving IPO: {}", ipoEntity);
                ipoRepository.save(ipoEntity);
            });
            log.info("All IPOs saved successfully.");
        } else {
            log.warn("No IPOs found to save.");
        }
    }

    @Override
    public void notifyOpenIpos() {
        List<ApplicationUser> applicationUsers = applicationUserRepository.findAll();
        String subject = "Live IPO Notification - " + CommonUtil.formatDateWithSuffix(LocalDate.now());
        List<IpoData> allIpos = this.getIpos(Optional.of("ongoing"));
        // Filter for live IPOs closing in the next 2 days
        LocalDate today = LocalDate.now();
        LocalDate nextDay = today.plusDays(1);

        AtomicReference<List<IpoData>> liveIpos = new AtomicReference<>(allIpos.stream()
                .filter(ipo -> {
                    if (ipo.getEndAt() == null) return false;
                    try {
                        LocalDate endDate = LocalDate.parse(ipo.getEndAt().substring(0, 10));
                        return (endDate.isEqual(today) || endDate.isAfter(today)) &&
                                (endDate.isEqual(nextDay) || endDate.isBefore(nextDay));
                    } catch (Exception e) {
                        log.warn("Error parsing end date for IPO {}: {}", ipo.getSymbol(), e.getMessage());
                        return false;
                    }
                }).toList());
        applicationUsers.forEach(user -> {
            // Apply user preferences filter
            Optional<UserPreferences> userPreferences = userPreferencesRepository
                .getUserPreferencesByPreferenceAndUserId(DEFAULT_USER_PREFERENCES.IPO_NOTIFICATIONS, user.getId());

            if(userPreferences.isPresent()) {
                String preferenceValue = userPreferences.get().getValue();
                if(preferenceValue.equalsIgnoreCase("SME")) {
                    liveIpos.set(liveIpos.get().stream()
                            .filter(ipo -> "SME".equalsIgnoreCase(ipo.getSubType()))
                            .toList());
                } else if(preferenceValue.equalsIgnoreCase("MAINBOARD")) {
                    liveIpos.set(liveIpos.get().stream()
                            .filter(ipo -> "IPO".equalsIgnoreCase(ipo.getSubType()))
                            .toList());
                } else if(preferenceValue.equalsIgnoreCase("NONE")) {
                    log.info("User {} has opted out of IPO notifications.", user.getUserName());
                    return;
                }
            }

            if (liveIpos.get().isEmpty()) {
                log.info("No live IPOs closing in the next two days. Skipping notification for user: {}", user.getUserName());
                return;
            }

            String emailBody = this.getEmailBody(subject, liveIpos.get(), user.getFullName());
            log.info("Sending notification to: {} with {} IPOs", user.getUserName(), liveIpos.get().size());
            ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
            resendEmailPayload.setTo(new String[]{user.getUserName()});
            resendEmailPayload.setSubject(subject);
            resendEmailPayload.setHtml(emailBody);
            emailService.sendEmail(resendEmailPayload);
        });
        log.info("Notification for live IPOs closing in the next two days completed.");
    }

    private String getEmailBody(String subject, List<IpoData> liveIpos, String userName) {
        // Create inner content context
        Context reportContext = new Context();
        reportContext.setVariable("liveIpos", liveIpos);
        reportContext.setVariable("subject", subject);
        reportContext.setVariable("userName", userName);

        // Process the report template
        String contentHtml = templateEngine.process("email/live-ipo-notification.html", reportContext);

        // Wrap in base layout
        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);

        return templateEngine.process("email/base-layout", baseContext);
    }

    /**
     * Functional interface for Kite API operations that may throw EncTokenExpiredException
     */
    @FunctionalInterface
    private interface KiteApiOperation<T> {
        T execute() throws EncTokenExpiredException;
    }
}
