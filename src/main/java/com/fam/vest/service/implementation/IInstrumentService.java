package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.repository.InstrumentRepository;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.repository.WatchlistInstrumentRepository;
import com.fam.vest.service.InstrumentService;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.util.InstrumentFormatter;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IInstrumentService implements InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final KiteConnector kiteConnector;
    private final WatchlistInstrumentRepository watchlistInstrumentRepository;

    @Value("${fam.vest.app.internal.trading.user}")
    private String internalTradingUser;

    @Value("${fam.vest.app.instrument.save.batch.size}")
    private int instrumentSaveBatchSize;

    @Override
    public void fetchAndSaveInstruments() {
        log.info("Fetching and saving instruments from Kite");
        if(StringUtils.isBlank(internalTradingUser)) {
            log.error("Internal trading user is not configured, skipping instrument fetch");
            return;
        }
        try {
            TradingAccount tradingAccount = tradingAccountRepository.getTradingAccountByUserId(internalTradingUser);
            List<com.zerodhatech.models.Instrument> instruments = this.fetchInstrumentsFromKite(tradingAccount);
            log.info("Fetched total {} instruments from Kite", instruments.size());
            this.cleanupWatchlistInstruments();
            this.saveInstruments(instruments, tradingAccount);
        } catch (IOException e) {
            log.error("IOException while fetching instruments", e);
        } catch (KiteException e) {
            log.error("KiteException while fetching instruments", e);
        }
    }

    private void cleanupWatchlistInstruments() {
        log.info("Deleting expired watchlist instruments");
        watchlistInstrumentRepository.deleteExpiredWatchlistInstruments();
        log.info("Expired watchlist instruments deleted");
    }


    private List<com.zerodhatech.models.Instrument> fetchInstrumentsFromKite(TradingAccount tradingAccount) throws IOException, KiteException {
        log.info("Fetching instruments from Kite");
        KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
        return kiteConnect.getInstruments();
    }

    @Transactional
    public void saveInstruments(List<com.zerodhatech.models.Instrument> instruments, TradingAccount tradingAccount) {
        if (instruments == null || instruments.isEmpty()) {
            log.warn("No instruments fetched from Kite, skipping save");
            return;
        }
        log.info("Deleting existing instruments from local database");
        instrumentRepository.deleteAll();
        log.info("Existing instruments deleted from local database");
        Set<String> seen = new HashSet<>();
        List<Instrument> instrumentList = new ArrayList<>(instruments.size());
        try {
            instruments.forEach(instrument -> {
                Instrument instrumentDetails = new Instrument();
                Date currentDate = Calendar.getInstance().getTime();
                instrumentDetails.setInstrumentToken(instrument.getInstrument_token());
                instrumentDetails.setExchangeToken(instrument.getExchange_token());
                instrumentDetails.setTradingSymbol(instrument.getTradingsymbol());
                instrumentDetails.setDisplayName(InstrumentFormatter.formatInstrument(instrument.getTradingsymbol(), instrument.getInstrument_type()));
                instrumentDetails.setName(instrument.getName());
                instrumentDetails.setLastPrice(instrument.getLast_price());
                instrumentDetails.setExpiry(instrument.getExpiry());
                instrumentDetails.setTickSize(instrument.getTick_size());
                instrumentDetails.setLotSize(instrument.getLot_size());
                instrumentDetails.setInstrumentType(instrument.getInstrument_type());
                instrumentDetails.setSegment(instrument.getSegment());
                instrumentDetails.setExchange(instrument.getExchange());
                instrumentDetails.setStrike(instrument.getStrike());
                instrumentDetails.setCreatedBy(tradingAccount.getUserId());
                instrumentDetails.setCreatedDate(currentDate);
                instrumentDetails.setLastModifiedBy(tradingAccount.getUserId());
                instrumentDetails.setLastModifiedDate(currentDate);
                String key = instrument.getExchange() + "-" + instrument.getTradingsymbol(); // create unique key
                if (seen.add(key)) { // add returns false if already present
                    instrumentList.add(instrumentDetails);
                } else {
                    log.warn("Duplicate instrument found: {} for exchange: {}. Skipping this instrument.", instrument.getTradingsymbol(), instrument.getExchange());
                }
            });
            log.info("Saving instruments to local database");
            int batchSize = instrumentSaveBatchSize;
            log.info("Splitting instruments into batches of size: {}", batchSize);
            for (int i = 0; i < instrumentList.size(); i += batchSize) {
                try {
                    int end = Math.min(i + batchSize, instrumentList.size());
                    List<Instrument> batch = instrumentList.subList(i, end);
                    log.debug("Saving batch of instruments from index {} to {}", i, end);
                    instrumentRepository.saveAll(batch);
                    instrumentRepository.flush(); // flush after every batch
                } catch (Exception exception) {
                    log.error("Exception while calling saveAll for instruments", exception);
                }
            }
            log.info("Saving instruments to local database completed successfully");
        } catch (Exception exception) {
            log.error("Exception while saving instrument", exception);
        }
    }

    @Override
    public Instrument getByTradingSymbol(String tradingSymbol) {
        Optional<Instrument> instrument = instrumentRepository.findByTradingSymbol(tradingSymbol);
        return instrument.orElseThrow(() -> new ResourceNotFoundException("Instrument not found for symbol: " + tradingSymbol));
    }

    @Override
    public Instrument getByTradingSymbolAndExchange(String tradingSymbol, String exchange) {
        Optional<Instrument> instrument = instrumentRepository.findByTradingSymbolAndExchange(tradingSymbol, exchange);
        return instrument.orElseThrow(() -> new ResourceNotFoundException("Instrument not found for symbol: " + tradingSymbol+" and exchange: " + exchange));
    }

    @Override
    public Instrument getByInstrumentToken(Long instrumentToken) {
        Optional<Instrument> instrument = instrumentRepository.findByInstrumentToken(instrumentToken);
        return instrument.orElseThrow(() -> new ResourceNotFoundException("Instrument not found for instrument token: " + instrumentToken));
    }

    @Override
    public List<Instrument> getInstruments() {
        return instrumentRepository.findAll();
    }
}
