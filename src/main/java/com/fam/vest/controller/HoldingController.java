package com.fam.vest.controller;

import com.fam.vest.dto.response.HoldingDetails;
import com.fam.vest.service.HoldingService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/holdings")
@CrossOrigin
public class HoldingController {

    private final HoldingService holdingService;

    @Autowired
    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping()
    public ResponseEntity<Object> getHoldings(@RequestParam(value = "type", required = false) Optional<String> type,
                                              @RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching holdings for type: {}, tradingAccountId: {} by: {}", type.orElse("all"), tradingAccountId.orElse("all"), userDetails.getUsername());
        List<HoldingDetails> holdings = holdingService.getHoldings(userDetails, type, tradingAccountId);
        return CommonUtil.success(holdings);
    }
}
