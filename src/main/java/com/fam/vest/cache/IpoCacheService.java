package com.fam.vest.cache;

import com.fam.vest.config.KiteInternalAPIConnector;
import com.fam.vest.dto.response.IpoResponse;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.EncTokenExpiredException;
import com.fam.vest.pojo.MutualFundNav;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.util.KITE_INTERNAL_API;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpoCacheService {

    private final RedisTemplate<String, IpoResponse> redisTemplate;
    private final TradingAccountRepository tradingAccountRepository;
    private final KiteInternalAPIConnector kiteInternalAPIConnector;

    @Value("${fam.vest.app.internal.trading.user}")
    private String internalTradingUser;

    private static final String IPO_CACHE_KEY = "kite-ipo-response";
    private static final long IPO_CACHE_TTL = 24; // hours

    @PostConstruct
    public void init() {
        clearIpoCache();
        log.info("IPO cache cleared on application startup.");
    }

    public void cacheIpos(IpoResponse ipoResponse) {
        try {
            redisTemplate.opsForValue().set(IPO_CACHE_KEY, ipoResponse, IPO_CACHE_TTL, TimeUnit.HOURS);
            log.debug("Caching ipos to Redis with key: {} size: {}", IPO_CACHE_KEY, ipoResponse.getData().size());
        } catch (Exception exception) {
            log.error("Error while caching Ipos to Redis: {}", exception.getMessage());
        }
    }

    public IpoResponse getIpos() {
        IpoResponse ipoResponse = null;
        try {
            ipoResponse = this.getCachedIpos();
            if(null == ipoResponse) {
                ipoResponse = this.getIposFromKiteAPI();
                this.cacheIpos(ipoResponse);
            }
        } catch (Exception exception) {
            log.error("Error while fetching ipos from Redis: {}", exception.getMessage());
        }
        return ipoResponse;
    }

    public void refreshIposFromKiteInternalApi() {
        log.debug("Refreshing IPOs from Kite Internal API...");
        clearIpoCache();
        IpoResponse ipoResponse = this.getIposFromKiteAPI();
        this.cacheIpos(ipoResponse);
    }

    private IpoResponse getCachedIpos() {
        return redisTemplate.opsForValue().get(IPO_CACHE_KEY);
    }

    private void clearIpoCache() {
        try {
            redisTemplate.delete(IPO_CACHE_KEY);
            log.debug("Cleared IPO cache in Redis with key: {}", IPO_CACHE_KEY);
        } catch (Exception exception) {
            log.error("Error while clearing IPO cache in Redis: {}", exception.getMessage());
        }
    }

    private IpoResponse getIposFromKiteAPI() {
        log.debug("Getting ipos from Kite Internal API...");
        if(StringUtils.isBlank(internalTradingUser)) {
            log.error("Internal trading user not set, cannot fetch IPOs");
            return new IpoResponse();
        }
        TradingAccount tradingAccount = tradingAccountRepository.getTradingAccountByUserId(internalTradingUser);
        IpoResponse ipoResponse;
        try {
            ipoResponse = kiteInternalAPIConnector.get(KITE_INTERNAL_API.GET_IPOS, tradingAccount, IpoResponse.class);
        } catch (EncTokenExpiredException encTokenExpiredException) {
            //enc token is present but expired, so we will only give one more attempt to call this api
            log.info("encToken expired for account: {}, retrying...", tradingAccount.getUserId());
            try {
                ipoResponse = kiteInternalAPIConnector.get(KITE_INTERNAL_API.GET_IPOS, tradingAccount, IpoResponse.class);
            } catch (EncTokenExpiredException ex) {
                log.error("Failed to retrieve IPOs after refreshing encToken for account: {}", tradingAccount.getUserId());
                return new IpoResponse();
            }
        }
        return ipoResponse;
    }
}
