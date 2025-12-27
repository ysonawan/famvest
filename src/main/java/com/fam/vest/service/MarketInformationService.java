package com.fam.vest.service;

import com.fam.vest.pojo.ExchangeStatusResponse;
import com.fam.vest.pojo.ExchangeTimingResponse;

import java.time.LocalDate;

public interface MarketInformationService {

    ExchangeTimingResponse getExchangeTradingTime(LocalDate date);

    ExchangeStatusResponse getExchangeStatus(String exchange);
}
