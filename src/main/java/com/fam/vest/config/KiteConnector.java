package com.fam.vest.config;

import ch.qos.logback.core.util.StringUtil;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.InvalidTokenException;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.TokenService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.TokenException;
import com.zerodhatech.models.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class KiteConnector {

    private ConcurrentHashMap<String, KiteConnect> kiteConnects;
    private final TradingAccountRepository tradingAccountRepository;
    private final TokenService tokenService;

    public KiteConnector(TradingAccountRepository tradingAccountRepository,
                         TokenService tokenService) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.tokenService = tokenService;
    }

    @PostConstruct
    public void init() {
        // Initialize the KiteConnect map or any other necessary setup
        log.info("Resetting kiteConnect instances and request tokens for all users on startup");
        kiteConnects = new ConcurrentHashMap<>();
        tradingAccountRepository.resetRequestTokens();
        int delay = 20;
        log.info("Scheduling request token renewal {} seconds after startup", delay);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                tokenService.renewRequestTokens(null, true);
            } catch (Exception e) {
                log.error("Error while renewing request tokens: {}", e.getMessage(), e);
            }
        }, delay, TimeUnit.SECONDS);
    }

    public KiteConnect getKiteConnect(TradingAccount tradingAccount) throws InternalException, InvalidTokenException {
        if (tradingAccount == null) {
            throw new IllegalArgumentException("Trading account user cannot be null or empty");
        }
        if(StringUtil.isNullOrEmpty(tradingAccount.getRequestToken())) {
            log.warn("Request token is null or empty for userId: {}", tradingAccount.getUserId());
            throw new RequestTokenMissingException("Request token is null or empty for userId: "+ tradingAccount.getUserId());
        }
        // Use synchronized block for thread safety
        synchronized (this) {
            KiteConnect kiteConnect = kiteConnects.get(tradingAccount.getUserId());
            if (kiteConnect == null) {
                // If KiteConnect is not present, create a new instance and store it in the map
                kiteConnect = this.initializeKiteConnect(tradingAccount);
                kiteConnects.put(tradingAccount.getUserId(), kiteConnect);
            }
            return kiteConnect;
        }
    }

    public KiteConnect resetKiteConnect(TradingAccount tradingAccount) throws InternalException, InvalidTokenException {
        kiteConnects.remove(tradingAccount.getUserId());
        return this.getKiteConnect(tradingAccount);
    }

    private KiteConnect initializeKiteConnect(TradingAccount tradingAccount) throws InternalException, InvalidTokenException {
        try {
            log.info("Initializing KiteConnect for userId: {}", tradingAccount.getUserId());
            KiteConnect kiteConnect = new KiteConnect(tradingAccount.getApiKey(), true);
            kiteConnect.setUserId(tradingAccount.getUserId());
            // Set session expiry callback.
            kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
                @Override
                public void sessionExpired() {
                    log.info("Session expired");
                }
            });
            log.info("Generating access token using request token and api secret");
            User user = kiteConnect.generateSession(tradingAccount.getRequestToken(), tradingAccount.getApiSecret());
            kiteConnect.setAccessToken(user.accessToken);
            kiteConnect.setPublicToken(user.publicToken);
            log.info("Access token generated successfully for userId: {}", tradingAccount.getUserId());
            return kiteConnect;
        } catch (KiteException e) {
            if(e instanceof TokenException) {
                log.error("TokenException: {}", e.getMessage());
                throw new InvalidTokenException(e.getMessage());
            } else {
                log.error("KiteException: {}", e.getMessage());
                throw new InternalException(e.getMessage());
            }
        } catch (IOException e) {
            log.error("IOException: {}", e.getMessage());
            throw new InternalException(e.getMessage());
        }
    }
}
