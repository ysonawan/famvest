package com.fam.vest.config;

import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.EncTokenExpiredException;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.TokenService;
import com.zerodhatech.kiteconnect.KiteConnect;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class KiteInternalAPIConnector {

    private final TradingAccountRepository tradingAccountRepository;
    private final TokenService tokenService;

    private String ZERODHA_BASE_URL="https://kite.zerodha.com/oms";


    public KiteInternalAPIConnector(TradingAccountRepository tradingAccountRepository,
                                    TokenService tokenService) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.tokenService = tokenService;
    }

    public <T> T exchange(String url, HttpMethod method, TradingAccount tradingAccount,
                          Class<T> responseType, Object payload) {
        try {
            if(StringUtils.isEmpty(tradingAccount.getEncToken())) {
                log.info("encToken is missing for account {}, refreshing token", tradingAccount.getUserId());
                this.refreshEncToken(tradingAccount);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "enctoken " + tradingAccount.getEncToken());
            headers.add("Cookie", "enctoken=" + tradingAccount.getEncToken());
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<?> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    ZERODHA_BASE_URL + url,
                    method, entity, responseType
            );
            return response.getBody();
        }  catch (org.springframework.web.client.HttpClientErrorException.Forbidden ex) {
            log.warn("403 Forbidden: encToken expired or unauthorized access for account {}", tradingAccount.getUserId());
            this.refreshEncToken(tradingAccount);
            throw new EncTokenExpiredException("encToken expired for account " + tradingAccount.getUserId());
        }
    }

    public <T> T get(String url, TradingAccount tradingAccount, Class<T> responseType) {
        return exchange(url, HttpMethod.GET, tradingAccount, responseType, null);
    }

    public <T> T post(String url, TradingAccount tradingAccount, Class<T> responseType, Object payload) {
        return exchange(url, HttpMethod.POST, tradingAccount, responseType, payload);
    }

    public <T> T delete(String url, TradingAccount tradingAccount, Class<T> responseType) {
        return exchange(url, HttpMethod.DELETE, tradingAccount, responseType, null);
    }

    private void refreshEncToken(TradingAccount tradingAccount) {
        String encToken = tokenService.getENCToken(tradingAccount);
        tradingAccount.setEncToken(encToken);
        tradingAccountRepository.save(tradingAccount);
        log.info("New enc token is refreshed for the user {}", tradingAccount.getUserId());
    }

}
