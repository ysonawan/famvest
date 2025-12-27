package com.fam.vest.controller;

import com.fam.vest.entity.Instrument;
import com.fam.vest.service.InstrumentService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping()
    public ResponseEntity<Object> getInstruments(@RequestParam(value = "instrumentToken", required = false) Optional<Long> instrumentToken,
                                                 @RequestParam(value = "tradingSymbol", required = false) Optional<String> tradingSymbol,
                                                 @RequestParam(value = "exchange", required = false) Optional<String> exchange) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        if(tradingSymbol.isPresent() && exchange.isPresent()) {
            log.info("Fetching instrument for trading symbol: {} and exchange: {} by: {}", tradingSymbol.get(), exchange.get(), userDetails.getUsername());
            Instrument instrument = instrumentService.getByTradingSymbolAndExchange(tradingSymbol.get(), exchange.get());
            return CommonUtil.success(instrument);
        } else  if(tradingSymbol.isPresent()) {
            log.info("Fetching instrument for trading symbol: {} by: {}", tradingSymbol.get(), userDetails.getUsername());
            Instrument instrument = instrumentService.getByTradingSymbol(tradingSymbol.get());
            return CommonUtil.success(instrument);
        }else if(instrumentToken.isPresent()) {
            log.info("Fetching instrument for instrument token: {} by: {}", instrumentToken.get(), userDetails.getUsername());
            Instrument instrument = instrumentService.getByInstrumentToken(instrumentToken.get());
            return CommonUtil.success(instrument);
        } else {
            log.info("Fetching all instruments by: {}", userDetails.getUsername());
            List<Instrument> instruments = instrumentService.getInstruments();
            return CommonUtil.success(instruments);
        }
    }
}
