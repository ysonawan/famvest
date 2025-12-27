package com.fam.vest.controller;

import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.dto.response.HoldingDetails;
import com.fam.vest.dto.response.MFOrderDetails;
import com.fam.vest.dto.response.MFSIPDetails;
import com.fam.vest.dto.request.SIPRequest;
import com.fam.vest.service.MutualFundService;
import com.fam.vest.util.RestResponse;
import com.fam.vest.util.UserDetailsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/mf")
@CrossOrigin
public class MutualFundController {

    private final MutualFundService mutualFundService;

    @Autowired
    public MutualFundController(MutualFundService mutualFundService) {
        this.mutualFundService = mutualFundService;
    }

    @GetMapping("/holdings")
    public ResponseEntity<Object> getHoldings(@RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching mf holdings for tradingAccountId: {} by: {}", tradingAccountId.orElse("all"), userDetails.getUsername());
        List<HoldingDetails> holdingDetails = mutualFundService.getMutualFundHoldings(userDetails, tradingAccountId);
        RestResponse<List<HoldingDetails>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), holdingDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/orders")
    public ResponseEntity<Object> getOrders(@RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching mf orders for tradingAccountId: {} by: {}", tradingAccountId.orElse("all"), userDetails.getUsername());
        List<MFOrderDetails> mfOrderDetails = mutualFundService.getConsolidatedMutualFundOrders(userDetails, tradingAccountId);
        RestResponse<List<MFOrderDetails>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
            String.valueOf(HttpStatus.OK.value()), mfOrderDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/sips")
    public ResponseEntity<Object> getSips(@RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching mf sips for tradingAccountId: {} by: {}", tradingAccountId.orElse("all"), userDetails.getUsername());
        List<MFSIPDetails> mfSipDetails = mutualFundService.getMutualFundSips(userDetails, tradingAccountId);
        RestResponse<List<MFSIPDetails>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), mfSipDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/sips/{tradingAccountId}/{sipId}")
    public ResponseEntity<Object> updateSip(@PathVariable String sipId,
                                            @PathVariable String tradingAccountId,
                                            @RequestBody SIPRequest sipRequest) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Updating mf sip for sipId: {}, tradingAccountId: {} by: {}", sipId, tradingAccountId, userDetails.getUsername());
        List<MFSIPDetails> mfSipDetails = mutualFundService.updateSip(sipId, tradingAccountId, userDetails, sipRequest);
        RestResponse<List<MFSIPDetails>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), mfSipDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
