package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.dto.response.PositionDetails;
import com.fam.vest.service.InstrumentService;
import com.fam.vest.service.PositionService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class IPositionService implements PositionService {

    private final TradingAccountService tradingAccountService;
    private final InstrumentService instrumentService;
    private final KiteConnector kiteConnector;

    @Autowired
    public IPositionService(TradingAccountService tradingAccountService,
                            InstrumentService instrumentService,
                            KiteConnector kiteConnector) {
        this.tradingAccountService = tradingAccountService;
        this.instrumentService  = instrumentService;
        this.kiteConnector = kiteConnector;
    }

    @Override
    public List<PositionDetails> getAllPositions() {
        return this.getPositions(null, Optional.ofNullable(null), Optional.ofNullable(null));
    }

    @Override
    public List<PositionDetails> getPositions(UserDetails userDetails, Optional<String> type, Optional<String> tradingAccountId) {
        List<TradingAccount> tradingAccounts = null;
        if(null == userDetails) {
            tradingAccounts = tradingAccountService.getAllTradingAccounts();
        } else {
            tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        }
        if(tradingAccountId.isPresent()) {
            tradingAccounts = tradingAccounts.stream().
                    filter(tradingAccount -> tradingAccount.getUserId().equals(tradingAccountId.get())).toList();
        }
        List<PositionDetails> positionDetails = new ArrayList<>();
        AtomicLong sequenceNumber = new AtomicLong(1);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                this.getPositions(type, kiteConnect, positionDetails, sequenceNumber);
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting positions for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return positionDetails;
    }

    private void getPositions(Optional<String> type, KiteConnect kiteConnect,
                              List<PositionDetails> positionDetails, AtomicLong sequenceNumber) throws KiteException, IOException {
        Map<String, List<Position>> positions = kiteConnect.getPositions();
        String positionType = type.orElse("net");
        positions.get(positionType).forEach(position -> {
            PositionDetails positionDetail = this.convertToPositionDetails(position, positionType, kiteConnect.getUserId());
            positionDetail.setSequenceNumber(sequenceNumber.getAndIncrement());
            positionDetails.add(positionDetail);
        });
    }

    private PositionDetails convertToPositionDetails(Position position, String positionType, String userId) {
        PositionDetails positionDetails = new PositionDetails();
        positionDetails.setType(positionType);
        positionDetails.setUserId(userId);
        positionDetails.setDayPnl(0.0);
        if (position.lastPrice != null) {
            Double dayPnl = (position.lastPrice - position.averagePrice) * position.netQuantity;
            positionDetails.setDayPnl(CommonUtil.round(dayPnl, 2));
        }
        positionDetails.setPosition(position);
        String displayName = position.tradingSymbol;
        try {
            Instrument instrument = instrumentService.getByTradingSymbolAndExchange(position.tradingSymbol, position.exchange);
            if(null != instrument) {
                displayName = instrument.getDisplayName();
            }
        } catch (ResourceNotFoundException e) {
            log.error("Instrument not found for symbol: {} exchange: {}", position.tradingSymbol, position.exchange);
        }
        positionDetails.setDisplayName(displayName);
        positionDetails.setInstrumentToken(Long.valueOf(position.instrumentToken));
        return positionDetails;
    }

}
