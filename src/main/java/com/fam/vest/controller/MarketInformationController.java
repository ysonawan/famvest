package com.fam.vest.controller;

import com.fam.vest.service.MarketInformationService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/rest/market")
public class MarketInformationController {

    private final MarketInformationService marketInformationService;

    @GetMapping("/timings")
    public ResponseEntity<Object> getMarketTimings(@RequestParam(value = "date", required = false) Optional<LocalDate> date) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        if(date.isEmpty()) {
            date = Optional.of(LocalDate.now());
        }
        log.info("Fetching market timings for date: {} by: {}", date, userDetails.getUsername());
        return CommonUtil.success(marketInformationService.getExchangeTradingTime(date.get()));
    }

    @GetMapping("/status/{exchange}")
    public ResponseEntity<Object> getMarketStatus(@PathVariable("exchange") String exchange) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching market status for exchange: {} by: {}", exchange, userDetails.getUsername());
        return CommonUtil.success(marketInformationService.getExchangeStatus(exchange));
    }
}
