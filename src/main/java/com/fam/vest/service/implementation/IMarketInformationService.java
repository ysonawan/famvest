package com.fam.vest.service.implementation;

import com.fam.vest.pojo.ExchangeStatusResponse;
import com.fam.vest.pojo.ExchangeTimingResponse;
import com.fam.vest.service.MarketInformationService;
import com.fam.vest.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Slf4j
@Service
public class IMarketInformationService implements MarketInformationService {

    private final RestTemplate restTemplate;

    @Value("${fam.vest.app.trading.time.api}")
    private String tradingTimeApi;

    @Value("${fam.vest.app.exchange.status.api}")
    private String exchangeStatusApi;

    public IMarketInformationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ExchangeTimingResponse getExchangeTradingTime(LocalDate date) {
        return restTemplate.getForObject(tradingTimeApi.replace("{date}", CommonUtil.formatDate(date)), ExchangeTimingResponse.class);
    }

    @Override
    public ExchangeStatusResponse getExchangeStatus(String exchange) {
        return restTemplate.getForObject(exchangeStatusApi.replace("{exchange}", exchange), ExchangeStatusResponse.class);
    }

}
