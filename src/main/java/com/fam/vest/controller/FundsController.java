package com.fam.vest.controller;

import com.fam.vest.dto.request.CombinedMarginCalculationRequest;
import com.fam.vest.dto.response.FundDetails;
import com.fam.vest.dto.request.MarginCalculationRequest;
import com.fam.vest.service.FundsService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import com.zerodhatech.models.CombinedMarginData;
import com.zerodhatech.models.MarginCalculationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/funds")
@CrossOrigin
public class FundsController {

    private final FundsService fundsService;

    public FundsController(FundsService fundsService) {
        this.fundsService = fundsService;
    }

    @GetMapping()
    public ResponseEntity<Object> getFunds(@RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching funds for tradingAccountId: {} by: {}", tradingAccountId.orElse("all"), userDetails.getUsername());
        List<FundDetails> fundDetails = fundsService.getFunds(userDetails, tradingAccountId);
        return CommonUtil.success(fundDetails);
    }

    @PostMapping("/margins/orders")
    public ResponseEntity<Object> getMarginCalculation(@RequestBody MarginCalculationRequest marginCalculationRequest) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching margin calculation for: {} by: {}", marginCalculationRequest, userDetails.getUsername());
        List<MarginCalculationData> marginData = fundsService.getMarginCalculation(userDetails, marginCalculationRequest);
        return CommonUtil.success(marginData);
    }

    @PostMapping("/margins/basket")
    public ResponseEntity<Object> getCombinedMarginCalculation(@RequestBody CombinedMarginCalculationRequest combinedMarginCalculationRequest) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching combined margin calculation for: {} by: {}", combinedMarginCalculationRequest, userDetails.getUsername());
        CombinedMarginData combinedMarginData = fundsService.getCombinedMarginCalculation(userDetails, combinedMarginCalculationRequest);
        return CommonUtil.success(combinedMarginData);
    }

}
