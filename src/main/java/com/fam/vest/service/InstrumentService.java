package com.fam.vest.service;

import com.fam.vest.entity.Instrument;

import java.util.List;

public interface InstrumentService {

    void fetchAndSaveInstruments();

    Instrument getByTradingSymbol(String tradingSymbol);

    Instrument getByTradingSymbolAndExchange(String symbol, String exchange);

    Instrument getByInstrumentToken(Long instrumentToken);

    List<Instrument> getInstruments();
}
