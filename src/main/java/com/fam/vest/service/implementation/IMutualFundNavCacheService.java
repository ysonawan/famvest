package com.fam.vest.service.implementation;

import com.fam.vest.pojo.MutualFundNav;
import com.fam.vest.service.MutualFundNavCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMutualFundNavCacheService implements MutualFundNavCacheService {

    private final RedisTemplate<String, MutualFundNav> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${fam.vest.app.mf.nav.api}")
    private String mutualFundNavApi;

    private static final String NAV_CACHE_KEY_PREFIX = "nav:";
    private static final long NAV_CACHE_TTL = 24; // hours

    @Override
    public void cacheMutualFundNav(String isin, MutualFundNav mutualFundNav) {
        try {
            redisTemplate.opsForValue().set(NAV_CACHE_KEY_PREFIX+isin, mutualFundNav, NAV_CACHE_TTL, TimeUnit.HOURS);
            log.debug("Caching mutual fund navs to Redis with key: {} value: {}", isin, mutualFundNav);
        } catch (Exception exception) {
            log.error("Error while caching NAV to Redis: {}", exception.getMessage());
        }
    }

    @Override
    public MutualFundNav getCachedMutualFundNav(String isin) {
        try {
            MutualFundNav nav = redisTemplate.opsForValue().get(NAV_CACHE_KEY_PREFIX+isin);
            if (nav != null) {
                log.debug("Getting mutual fund navs from Redis with key: {} value: {}", isin, nav);
                return nav;
            }
        } catch (Exception exception) {
            log.error("Error while fetching NAV from Redis: {}", exception.getMessage());
        }
        return null;
    }

    @Override
    public void emptyMutualFundNavCache() {
        var keys = redisTemplate.keys(NAV_CACHE_KEY_PREFIX+"*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Redis deleted keys: {}", keys);
        } else {
            log.info("No Nav keys to delete: {}", keys);
        }
    }

    @Override
    public void updateMutualFundNavCache() {
        var keys = redisTemplate.keys(NAV_CACHE_KEY_PREFIX+"*");
        if (!keys.isEmpty()) {
            keys.forEach(key -> {
                String isin = key.replace(NAV_CACHE_KEY_PREFIX, "");
                MutualFundNav mfNav = this.fetchMutualFundNavFromApi(isin);
                MutualFundNav cachedMutualFundNav = this.getCachedMutualFundNav(isin);
                if(null != mfNav) {
                    if(null == cachedMutualFundNav || (cachedMutualFundNav.getNav().getNav() != mfNav.getNav().getNav() ||
                            cachedMutualFundNav.getLastNav().getNav() != mfNav.getLastNav().getNav())) {
                        log.debug("Cached mutual fund nav not found or is updated for isin: {}. Updating cache with new nav: {}", key, mfNav.getNav());
                        this.cacheMutualFundNav(isin, mfNav);
                    }
                }
            });
        }
    }

    @Override
    public MutualFundNav getMutualFundNav(String isin) {
        // get mutual fund nav from external api
        MutualFundNav mfNav = this.getCachedMutualFundNav(isin);
        if (null == mfNav ) {
            mfNav = this.fetchMutualFundNavFromApi(isin);
            this.cacheMutualFundNav(isin, mfNav);
        }
        return mfNav;
    }

    private MutualFundNav fetchMutualFundNavFromApi(String isin) {
        // get mutual fund nav from external api
        MutualFundNav mfNav = null;
        MutualFundNav[] mfNavResponse = restTemplate.getForObject(mutualFundNavApi.replace("{isin}", isin), MutualFundNav[].class);
        if (null != mfNavResponse && mfNavResponse[0].getNav() != null && mfNavResponse[0].getLastNav() != null) {
            mfNav = mfNavResponse[0];
        }
        return mfNav;
    }
}
