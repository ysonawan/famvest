package com.fam.vest.controller;

import com.fam.vest.dto.request.IpoBidRequest;
import com.fam.vest.service.IpoService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest/v1/ipos")
@CrossOrigin
@RequiredArgsConstructor
public class IpoController {

    private final IpoService ipoService;

    @GetMapping()
    public ResponseEntity<Object> getIPOs(@RequestParam(value = "status", required = false) Optional<String> status) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching IPOs for status: {} by: {}", status.orElse("all"), userDetails.getUsername());
        return CommonUtil.success(ipoService.getIpos(status));
    }

    @GetMapping("/applications")
    public ResponseEntity<Object> getIpoApplications(@RequestParam(value = "userId", required = false) Optional<String> tradingAccountId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching IPO applications for user id: {} by: {}", tradingAccountId.orElse("all"), userDetails.getUsername());
        return CommonUtil.success(ipoService.getIpoApplications(tradingAccountId, userDetails));
    }

    @GetMapping("/{tradingAccountId}/vpa")
    public ResponseEntity<Object> getVPA(@PathVariable String tradingAccountId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching VPA for user id: {} by: {}", tradingAccountId ,userDetails.getUsername());
        return CommonUtil.success(ipoService.getVPA(tradingAccountId, userDetails));
    }

    @PostMapping("/{tradingAccountId}/applications")
    public ResponseEntity<Object> submitIpoApplication(@PathVariable String tradingAccountId, @RequestBody IpoBidRequest ipoBidRequest) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Submitting IPO application for user id: {} by: {}", tradingAccountId ,userDetails.getUsername());
        return CommonUtil.success(ipoService.submitIpoApplication(tradingAccountId, userDetails, ipoBidRequest));
    }

    @DeleteMapping("/{tradingAccountId}/applications/{applicationId}")
    public ResponseEntity<Object> cancelIpoApplication(@PathVariable String tradingAccountId,@PathVariable String applicationId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Cancelling IPO application for user id: {} application id: {} by: {}", tradingAccountId, applicationId ,userDetails.getUsername());
        return CommonUtil.success(ipoService.cancelIpoApplication(tradingAccountId, userDetails, applicationId));
    }
}
