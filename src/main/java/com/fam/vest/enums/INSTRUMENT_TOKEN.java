package com.fam.vest.enums;

public enum INSTRUMENT_TOKEN {

    NIFTY_50("NSE", "NIFTY 50", 256265L),
    BANK_NIFTY("NSE", "BANK NIFTY", 260105L),
    SENSEX("BSE", "SENSEX",265L);

    private final String exchange;
    private final String symbol;
    private final Long token;

    INSTRUMENT_TOKEN(String exchange, String symbol, Long token) {
        this.exchange = exchange;
        this.symbol = symbol;
        this.token = token;
    }

    public String getExchange() {
        return exchange;
    }

    public String getSymbol() {
        return symbol;
    }

    public Long getToken() {
        return token;
    }

    public static INSTRUMENT_TOKEN fromSymbol(String symbol) {
        for (INSTRUMENT_TOKEN value : INSTRUMENT_TOKEN.values()) {
            if (value.symbol.equals(symbol)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant for symbol: " + symbol);
    }
}
