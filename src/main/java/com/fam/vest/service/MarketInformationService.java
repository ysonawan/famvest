package com.fam.vest.service;

import com.fam.vest.pojo.ExchangeStatusResponse;
import com.fam.vest.pojo.ExchangeTimingResponse;
import com.fam.vest.pojo.MarketHolidaysResponse;

import java.time.LocalDate;

public interface MarketInformationService {

    ExchangeTimingResponse getExchangeTradingTime(LocalDate date);

    ExchangeStatusResponse getExchangeStatus(String exchange);

    MarketHolidaysResponse getMarketHolidays();
}
