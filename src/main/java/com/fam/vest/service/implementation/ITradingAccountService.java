package com.fam.vest.service.implementation;

import ch.qos.logback.core.util.StringUtil;
import com.fam.vest.config.KiteConnector;
import com.fam.vest.dto.request.UpdateTradingAccountRequestDto;
import com.fam.vest.dto.response.TradingAccountResponseDto;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.ApplicationUserTradingAccountId;
import com.fam.vest.entity.ApplicationUserTradingAccountMapping;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.*;
import com.fam.vest.dto.request.TradingAccountRequestDto;
import com.fam.vest.dto.response.UserProfile;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.repository.ApplicationUserTradingAccountMappingRepository;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.TokenService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Profile;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ITradingAccountService implements TradingAccountService {

    private final TradingAccountRepository tradingAccountRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final ApplicationUserTradingAccountMappingRepository applicationUserTradingAccountMappingRepository;
    private final KiteConnector kiteConnector;
    private final TokenService tokenService;

    @Autowired
    public ITradingAccountService(TradingAccountRepository tradingAccountRepository,
                                  ApplicationUserRepository applicationUserRepository,
                                  ApplicationUserTradingAccountMappingRepository applicationUserTradingAccountMappingRepository,
                                  KiteConnector kiteConnector,
                                  TokenService tokenService) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.applicationUserRepository = applicationUserRepository;
        this.applicationUserTradingAccountMappingRepository = applicationUserTradingAccountMappingRepository;
        this.kiteConnector = kiteConnector;
        this.tokenService = tokenService;
    }

    @Value("${fam.vest.app.kite.login.endpoint}")
    private String kiteLoginEndpoint;

    @Override
    public List<TradingAccount> getTradingAccounts(UserDetails userDetails, boolean onlyActive) {
        List<TradingAccount> activeTradingAccounts;
                ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        if(null != applicationUser) {
            List<TradingAccount> tradingAccounts = applicationUserTradingAccountMappingRepository.findTradingAccountsByApplicationUserId(applicationUser.getId());
            if(onlyActive) {
                activeTradingAccounts =  tradingAccounts.stream().filter(TradingAccount::getIsActive).toList();
            } else {
                activeTradingAccounts = tradingAccounts;
            }
        } else {
            log.error("Application user not found for username: {}", userDetails.getUsername());
            throw new ResourceNotFoundException("Application user not found for username: " + userDetails.getUsername());
        }
        return activeTradingAccounts;
    }

    @Override
    public List<TradingAccount> getTradingAccounts(ApplicationUser applicationUser, boolean onlyActive) {
        List<TradingAccount> activeTradingAccounts;
        List<TradingAccount> tradingAccounts = applicationUserTradingAccountMappingRepository.findTradingAccountsByApplicationUserId(applicationUser.getId());
        if(onlyActive) {
            activeTradingAccounts =  tradingAccounts.stream().filter(TradingAccount::getIsActive).toList();
        } else {
            activeTradingAccounts = tradingAccounts;
        }
        return activeTradingAccounts;
    }

    @Override
    public List<TradingAccount> getAllTradingAccounts() {
        return tradingAccountRepository.findAll();
    }

    @Override
    @Transactional
    public TradingAccountResponseDto onboardTradingAccount(UserDetails userDetails, TradingAccountRequestDto tradingAccountRequestDto) {
        TradingAccount tradingAccountById = tradingAccountRepository.findTradingAccountByUserId(tradingAccountRequestDto.getUserId());
        TradingAccount tradingAccountByName = tradingAccountRepository.findTradingAccountByName(tradingAccountRequestDto.getName());
        if (tradingAccountById != null || tradingAccountByName != null) {
            log.error("Trading account already exists with userId: {} or name: {}", tradingAccountRequestDto.getUserId(), tradingAccountRequestDto.getName());
            throw new ResourceAlreadyExistException("Trading account already exists with userId: " + tradingAccountRequestDto.getUserId() + " or name: " + tradingAccountRequestDto.getName());
        }
        Date currentDate = Calendar.getInstance().getTime();
        TradingAccount tradingAccount = new TradingAccount();
        tradingAccount.setUserId(tradingAccountRequestDto.getUserId());
        tradingAccount.setName(tradingAccountRequestDto.getName());
        tradingAccount.setPassword(tradingAccountRequestDto.getPassword());
        tradingAccount.setTotpKey(tradingAccountRequestDto.getTotpKey());
        tradingAccount.setApiKey(tradingAccountRequestDto.getApiKey());
        tradingAccount.setApiSecret(tradingAccountRequestDto.getApiSecret());
        tradingAccount.setCreatedBy(userDetails.getUsername());
        tradingAccount.setCreatedDate(currentDate);
        tradingAccount.setLastModifiedBy(userDetails.getUsername());
        tradingAccount.setLastModifiedDate(currentDate);
        tradingAccount.setIsActive(Boolean.TRUE);
        log.info("Saving trading account for: {}", tradingAccount.getUserId());
        TradingAccount savedTradingAccount = tradingAccountRepository.save(tradingAccount);
        //create application user and trading account mapping
        ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());
        ApplicationUserTradingAccountMapping applicationUserTradingAccountMapping = new ApplicationUserTradingAccountMapping();
        applicationUserTradingAccountMapping.setTradingAccount(savedTradingAccount);
        applicationUserTradingAccountMapping.setApplicationUser(applicationUser);
        applicationUserTradingAccountMapping.setCreatedBy(userDetails.getUsername());
        applicationUserTradingAccountMapping.setCreatedDate(currentDate);
        applicationUserTradingAccountMapping.setLastModifiedBy(userDetails.getUsername());
        applicationUserTradingAccountMapping.setLastModifiedDate(currentDate);
        log.info("Saving application user and trading account mapping for: {} and : {}", savedTradingAccount.getUserId(), userDetails.getUsername());
        applicationUserTradingAccountMappingRepository.save(applicationUserTradingAccountMapping);
        tokenService.renewRequestToken(savedTradingAccount.getUserId());
        return this.getTradingAccountResponseDto(savedTradingAccount);
    }

    @Override
    public TradingAccountResponseDto updateTradingAccount(UserDetails userDetails, String accountUserId, UpdateTradingAccountRequestDto tradingAccountRequestDto) {
        TradingAccount tradingAccount = this.getTradingAccount(userDetails, accountUserId);
        List<TradingAccount> tradingAccounts = this.getTradingAccounts(userDetails, false);
        boolean exists = tradingAccounts.stream().anyMatch(ta ->
                (ta.getUserId().equals(tradingAccountRequestDto.getUserId()) || ta.getName().equals(tradingAccountRequestDto.getName()))) &&
                !tradingAccount.getId().equals(tradingAccountRequestDto.getId());
        if (exists) {
            log.error("Modify Trading Account: Trading account already exists with userId: {} or name: {} for: {}", tradingAccountRequestDto.getUserId(), tradingAccountRequestDto.getName(), userDetails.getUsername());
            throw new ResourceAlreadyExistException("You already have trading account with userId: " + tradingAccountRequestDto.getUserId() + " or name: " + tradingAccountRequestDto.getName());
        }
        tradingAccount.setId(tradingAccountRequestDto.getId());
        tradingAccount.setName(tradingAccountRequestDto.getName());

        //update trading user only for fields which are not empty
        if (!StringUtil.isNullOrEmpty(tradingAccountRequestDto.getPassword())) {
            tradingAccount.setPassword(tradingAccountRequestDto.getPassword());
        }
        if (!StringUtil.isNullOrEmpty(tradingAccountRequestDto.getTotpKey())) {
            tradingAccount.setTotpKey(tradingAccountRequestDto.getTotpKey());
        }
        if (!StringUtil.isNullOrEmpty(tradingAccountRequestDto.getApiKey())) {
            tradingAccount.setApiKey(tradingAccountRequestDto.getApiKey());
        }
        if (!StringUtil.isNullOrEmpty(tradingAccountRequestDto.getApiSecret())) {
            tradingAccount.setApiSecret(tradingAccountRequestDto.getApiSecret());
        }
        Date currentDate = Calendar.getInstance().getTime();
        tradingAccount.setLastModifiedBy(userDetails.getUsername());
        tradingAccount.setLastModifiedDate(currentDate);
        TradingAccount updatedTradingAccount = tradingAccountRepository.save(tradingAccount);
        tokenService.renewRequestToken(updatedTradingAccount.getUserId());
        return this.getTradingAccountResponseDto(updatedTradingAccount);
    }

    @Override
    public TradingAccountResponseDto mapUnmapTradingAccount(UserDetails userDetails, String tradingAccountId, boolean isUnmappedRequest) {
        TradingAccount tradingAccount = this.getTradingAccount(userDetails, tradingAccountId);
        tradingAccount.setIsActive(!isUnmappedRequest);
        TradingAccount updatedTradingAccount = tradingAccountRepository.save(tradingAccount);
        return this.getTradingAccountResponseDto(updatedTradingAccount);
    }

    @Override
    @Transactional
    public void deleteTradingAccount(UserDetails userDetails, String tradingAccountId) {
        TradingAccount tradingAccount = this.getTradingAccount(userDetails, tradingAccountId);
        ApplicationUser applicationUser =  applicationUserRepository.findApplicationUserByUserName(userDetails.getUsername());

        ApplicationUserTradingAccountId compositeId = new ApplicationUserTradingAccountId(applicationUser.getId(), tradingAccount.getId());
        Optional<ApplicationUserTradingAccountMapping> applicationUserTradingAccountMapping = applicationUserTradingAccountMappingRepository.findById(compositeId);
        if(applicationUserTradingAccountMapping.isPresent()) {
            log.info("Deleting application user and trading account mapping for: {} and : {}", tradingAccount.getUserId(), userDetails.getUsername());
            applicationUserTradingAccountMappingRepository.delete(applicationUserTradingAccountMapping.get());
        }
        log.info("Deleting trading account: {}", tradingAccount.getUserId());
        tradingAccountRepository.delete(tradingAccount);
    }

    private TradingAccountResponseDto getTradingAccountResponseDto(TradingAccount tradingAccount) {
        TradingAccountResponseDto tradingAccountResponseDto = new TradingAccountResponseDto();
        tradingAccountResponseDto.setId(tradingAccount.getId());
        tradingAccountResponseDto.setName(tradingAccount.getName());
        tradingAccountResponseDto.setUserId(tradingAccount.getUserId());
        tradingAccountResponseDto.setCreatedBy(tradingAccount.getCreatedBy());
        tradingAccountResponseDto.setCreatedDate(tradingAccount.getCreatedDate());
        tradingAccountResponseDto.setLastModifiedBy(tradingAccount.getLastModifiedBy());
        tradingAccountResponseDto.setLastModifiedDate(tradingAccount.getLastModifiedDate());
        tradingAccountResponseDto.setActive(tradingAccount.getIsActive());
        return tradingAccountResponseDto;
    }

    @Override
    public TradingAccount getTradingAccount(UserDetails userDetails, String tradingAccountUserId) {
        this.validateTradingAccountMapping(userDetails, tradingAccountUserId);
        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(tradingAccountUserId);
        if (tradingAccount == null) {
            log.error("Trading user not found for trading user id: {}", tradingAccountUserId);
            throw new ResourceNotFoundException("Trading user not found for trading user id: " + tradingAccountUserId);
        }
        return tradingAccount;
    }

    @Override
    public TradingAccountResponseDto getTradingAccountDto(UserDetails userDetails, String tradingAccountUserId) {
        this.validateTradingAccountMapping(userDetails, tradingAccountUserId);
        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(tradingAccountUserId);
        if (tradingAccount == null) {
            log.error("Trading user not found for trading user id: {}", tradingAccountUserId);
            throw new ResourceNotFoundException("Trading user not found for trading user id: " + tradingAccountUserId);
        }
        return this.getTradingAccountResponseDto(tradingAccount);
    }

    private TradingAccount getTradingAccount(String tradingAccountUserId) {
        return tradingAccountRepository.findTradingAccountByUserId(tradingAccountUserId);
    }

    @Override
    public List<UserProfile> getTradingAccountProfiles(UserDetails userDetails) {
        List<TradingAccount> tradingAccounts = this.getTradingAccounts(userDetails, false);
        List<UserProfile> profiles = new ArrayList<>();
        tradingAccounts.forEach(tradingAccount -> {
           UserProfile userProfile = this.getProfile(tradingAccount);
           profiles.add(userProfile);
        });
        return profiles;
    }

    private UserProfile getProfile(TradingAccount tradingAccount) {
        UserProfile userProfile = new UserProfile();
        userProfile.setActive(tradingAccount.getIsActive());
        userProfile.setId(tradingAccount.getId());
        userProfile.setName(tradingAccount.getName());
        userProfile.setUserId(tradingAccount.getUserId());
        userProfile.setKiteLoginEndPoint(this.getKiteLoginEndPoint(tradingAccount.getUserId()));
        userProfile.setTokenStatus("Not Available");
        try {
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            Profile profile = kiteConnect.getProfile();
            userProfile.setProfile(profile);
            userProfile.setTokenStatus("Valid");
        } catch (RequestTokenMissingException e) {
            log.warn("RequestToken missing for trading user: {}", tradingAccount.getUserId());
        } catch (IOException e) {
            log.error("Error fetching profile for userId: {}", tradingAccount.getUserId(), e);
        } catch (KiteException e) {
            log.error("Error fetching profile for userId: {}", tradingAccount.getUserId(), e);
        }
        return userProfile;
    }

    private void validateTradingAccountMapping(UserDetails userDetails, String tradingAccountUserId) {
        List<TradingAccount> tradingAccounts = this.getTradingAccounts(userDetails, false);
        if (tradingAccounts.stream().noneMatch(tradingAccount -> tradingAccount.getUserId().equals(tradingAccountUserId))) {
            log.error("Trading account is not associated with the user: {}", userDetails.getUsername());
            throw new ResourceNotFoundException("Trading account is not associated with the user");
        }
    }

    private String getKiteLoginEndPoint(String userId) {
        TradingAccount tradingAccount = this.getTradingAccount(userId);
        String endPoint = this.kiteLoginEndpoint.replace("{userId}", tradingAccount.getUserId()).
                replace("{name}", tradingAccount.getName());
        if(tradingAccount.getApiKey() != null) {
            endPoint = endPoint.replace("{apiKey}", tradingAccount.getApiKey());
        }
        return endPoint;
    }

    @Override
    public Profile registerRequestToken(String tradingAccountUserId, String requestToken, String status) {
        Profile profile;
        try {
            TradingAccount tradingAccount = this.getTradingAccount(tradingAccountUserId);
            tradingAccount.setRequestToken(requestToken);
            KiteConnect kiteConnect = kiteConnector.resetKiteConnect(tradingAccount);
            profile = kiteConnect.getProfile();
            tradingAccountRepository.save(tradingAccount);
        } catch (InvalidTokenException e) {
            log.error("InvalidTokenException in registerRequestToken: {}", e.getMessage());
            throw new InternalException(e.getMessage());
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while getting registering token request for trading user: {}. Error: {}", tradingAccountUserId, errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return profile;
    }

    @Override
    public String getTotp(UserDetails userDetails, String tradingAccountUserId) {
        try {
            validateTradingAccountMapping(userDetails, tradingAccountUserId);
            TradingAccount tradingAccount = this.getTradingAccount(tradingAccountUserId);

            if (tradingAccount.getTotpKey() == null || tradingAccount.getTotpKey().isEmpty()) {
                log.error("TOTP key not configured for trading account: {}", tradingAccountUserId);
                throw new ResourceNotFoundException("TOTP key is not configured for this trading account. TOTP cannot be generated.");
            }
            // Use TokenService to generate TOTP
            String totpPin = tokenService.getTotp(tradingAccount.getTotpKey());
            log.debug("TOTP retrieved successfully for trading account: {}", tradingAccountUserId);
            return totpPin;

        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Error while retrieving TOTP for trading account: {}", tradingAccountUserId, e);
            throw e;
        } catch (Exception e) {
            log.error("Error while executing TOTP script for trading account: {}", tradingAccountUserId, e);
            throw new InternalException("Error while retrieving TOTP: " + e.getMessage());
        }
    }

}
