package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.repository.InstrumentRepository;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.InternalTickFeedService;
import com.fam.vest.service.InternalTickSubscriptionService;
import com.fam.vest.service.QuoteService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;


@Slf4j
@Service
public class IQuoteService implements QuoteService {

    private final TradingAccountRepository tradingAccountRepository;
    private KiteConnector kiteConnector;
    private final InternalTickSubscriptionService internalTickSubscriptionService;
    private final InternalTickFeedService internalTickFeedService;
    private final InstrumentRepository instrumentRepository;

    public IQuoteService(TradingAccountRepository tradingAccountRepository,
                         KiteConnector kiteConnector,
                         InternalTickSubscriptionService internalTickSubscriptionService,
                         InternalTickFeedService internalTickFeedService,
                         InstrumentRepository instrumentRepository) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.kiteConnector = kiteConnector;
        this.internalTickSubscriptionService = internalTickSubscriptionService;
        this.internalTickFeedService = internalTickFeedService;
        this.instrumentRepository = instrumentRepository;
    }

    @Value("${fam.vest.app.data.streaming.user}")
    private String dataStreamingUser;

    @Value("${fam.vest.app.is.custom.data.streaming:true}")
    private boolean isCustomDataStreaming;

    @Override
    public Map<String, Quote> getQuote(String instrument) {
       return this.getQuotes(new String[]{instrument});
    }

    @Override
    public Map<String, Quote> getQuotes(String[] instruments) {
        Map<String, Quote> quotes = new HashMap<>();
        try {
            if(!isCustomDataStreaming) {
                TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(dataStreamingUser);
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                quotes = kiteConnect.getQuote(instruments);
            } else {
                quotes = this.fetchQuotesFromInternalTickFeedService(instruments);
            }
        } catch (RequestTokenMissingException requestTokenMissingException) {
            log.warn("Request token is missing for user: {}", dataStreamingUser);
        } catch (KiteException kiteException) {
            log.error("KiteException while fetching quotes using user {}: {}", dataStreamingUser, kiteException.getMessage());
            log.info("Trying to fetch quotes from internal tick feed service as fallback.");
            quotes = this.fetchQuotesFromInternalTickFeedService(instruments);
        } catch (IOException ioException) {
            log.error("IOException while fetching quotes using user {}: {}", dataStreamingUser, ioException.getMessage());
        }
        return quotes;
    }

    private Map<String, Quote> fetchQuotesFromInternalTickFeedService(String[] instruments) {
        Map<String, Quote> quotes = new HashMap<>();
        Set<Long> instrumentsToSubscribe = new HashSet<>();
        // First, check which instruments need subscription
        for (String instrument : instruments) {
            String tradingSymbol = instrument.split(":")[1];
            String exchange = instrument.split(":")[0];
            Optional<Instrument> instrumentOptional = instrumentRepository.findByTradingSymbolAndExchange(tradingSymbol, exchange);
            if (instrumentOptional.isPresent()) {
                Long instrumentToken = instrumentOptional.get().getInstrumentToken();
                Tick tick = internalTickFeedService.getLatestTick(instrumentToken);
                if (tick == null) {
                    // No tick available, need to subscribe
                    instrumentsToSubscribe.add(instrumentToken);
                } else {
                    // Tick is available, create quote directly
                    Quote quote = new Quote();
                    quote.instrumentToken = tick.getInstrumentToken();
                    quote.lastPrice = tick.getLastTradedPrice();
                    quotes.put(instrument, quote);
                }
            }
        }

        // Only subscribe if there are instruments that need subscription
        if (!instrumentsToSubscribe.isEmpty()) {
            internalTickSubscriptionService.subscribeToKiteWebsocket(instrumentsToSubscribe);

            //3 seconds sleep to allow websocket to connect and fetch latest ticks
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while waiting for websocket connection: {}", e.getMessage());
            }

            // Now fetch ticks for the subscribed instruments
            for (String instrument : instruments) {
                if (quotes.containsKey(instrument)) {
                    continue; // Already processed above
                }
                String tradingSymbol = instrument.split(":")[1];
                String exchange = instrument.split(":")[0];
                Optional<Instrument> instrumentOptional = instrumentRepository.findByTradingSymbolAndExchange(tradingSymbol, exchange);
                if (instrumentOptional.isPresent()) {
                    Long instrumentToken = instrumentOptional.get().getInstrumentToken();
                    Tick tick = internalTickFeedService.getLatestTick(instrumentToken);
                    if (tick != null) {
                        Quote quote = new Quote();
                        quote.instrumentToken = tick.getInstrumentToken();
                        quote.lastPrice = tick.getLastTradedPrice();
                        quotes.put(instrument, quote);
                    }
                }
            }
        }
        return quotes;
    }
}
