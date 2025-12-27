package com.fam.vest.controller;

import com.fam.vest.dto.request.HistoricalCandleDataRequest;
import com.fam.vest.entity.HistoricalTimelineValues;
import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.service.HistoricalCandleDataService;
import com.fam.vest.service.HistoricalTimelineService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.RestResponse;
import com.fam.vest.util.UserDetailsUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/history")
@CrossOrigin
public class HistoricalDataController {

    private final HistoricalCandleDataService historicalCandleDataService;
    private final HistoricalTimelineService historicalTimelineService;

    public HistoricalDataController(HistoricalCandleDataService historicalCandleDataService,
                                    HistoricalTimelineService historicalTimelineService) {
        this.historicalCandleDataService = historicalCandleDataService;
        this.historicalTimelineService = historicalTimelineService;
    }

    @PostMapping("/candles")
    public ResponseEntity<Object> getHistoricalCandleData(@Valid @RequestBody HistoricalCandleDataRequest request) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching historical candle data for request: {} by: {}", request, userDetails.getUsername());
        String csv = historicalCandleDataService.getHistoricalCandleData(request);
        return new ResponseEntity<>(new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, csv,
                String.valueOf(HttpStatus.OK.value()), null), HttpStatus.OK);
    }

    @GetMapping("/timelines/{type}")
    public ResponseEntity<Object> getHistoricalTimelineValues(@RequestParam(value = "userId", required = false) Optional<String> tradingAccountId,
                                                                      @PathVariable ("type") String type) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching historical time line values for tradingAccountId: {} by: {}", tradingAccountId.orElse("all"), userDetails.getUsername());
        List<HistoricalTimelineValues> historicalTimelineValues = historicalTimelineService.getHistoricalTimelineValues(userDetails, tradingAccountId, type);
        return CommonUtil.success(historicalTimelineValues);
    }
}
