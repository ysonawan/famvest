package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.dto.request.HistoricalCandleDataRequest;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.HistoricalCandleDataService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class IHistoricalCandleDataService implements HistoricalCandleDataService {

    private final KiteConnector kiteConnector;
    private final TradingAccountRepository tradingAccountRepository;

    public IHistoricalCandleDataService(KiteConnector kiteConnector,
                                        TradingAccountRepository tradingAccountRepository) {
        this.kiteConnector = kiteConnector;
        this.tradingAccountRepository = tradingAccountRepository;
    }

    @Value("${fam.vest.app.data.streaming.user}")
    private String dataStreamingUser;

    @Override
    public String getHistoricalCandleData(HistoricalCandleDataRequest historicalCandleDataRequest) {
        String csv;
        try {
            if(StringUtils.isEmpty(dataStreamingUser)) {
                log.error("Data streaming user is not configured. Please check your application properties.");
                throw new IllegalStateException("Data streaming user is not configured.");
            }
            TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(dataStreamingUser);
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            HistoricalData historicalData = kiteConnect.getHistoricalData(historicalCandleDataRequest.getFrom(), historicalCandleDataRequest.getTo(),
                    historicalCandleDataRequest.getInstrumentToken(), historicalCandleDataRequest.getInterval(),
                    historicalCandleDataRequest.getContinuous(), historicalCandleDataRequest.getOi());
            csv = CommonUtil.generateCsv(historicalData);
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while getting historical candle data by trading user: {}. Error: {}", dataStreamingUser, errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return csv;
    }
}
