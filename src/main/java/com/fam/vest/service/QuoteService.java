package com.fam.vest.service;

import com.zerodhatech.models.Quote;

import java.util.Map;

public interface QuoteService {

    Map<String, Quote> getQuote(String instrument);

    Map<String, Quote> getQuotes(String[] instruments);
}
