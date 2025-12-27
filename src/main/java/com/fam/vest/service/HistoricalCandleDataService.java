package com.fam.vest.service;


import com.fam.vest.dto.request.HistoricalCandleDataRequest;

public interface HistoricalCandleDataService {

    String getHistoricalCandleData(HistoricalCandleDataRequest historicalCandleDataRequest);
}
