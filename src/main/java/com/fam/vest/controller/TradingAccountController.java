package com.fam.vest.controller;

import com.fam.vest.dto.request.UpdateTradingAccountRequestDto;
import com.fam.vest.dto.response.TradingAccountResponseDto;
import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.dto.request.TradingAccountRequestDto;
import com.fam.vest.dto.response.UserProfile;
import com.fam.vest.service.TokenService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.RestResponse;
import com.fam.vest.util.UserDetailsUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rest/v1/accounts")
@CrossOrigin
public class TradingAccountController {

    private final TradingAccountService tradingAccountService;
    private final TokenService tokenService;

    @Autowired
    public TradingAccountController(TradingAccountService tradingAccountService,
                                    TokenService tokenService) {
        this.tradingAccountService = tradingAccountService;
        this.tokenService = tokenService;
    }

   @PostMapping()
    public ResponseEntity<Object> onboardTradingAccount(@Valid @RequestBody TradingAccountRequestDto tradingAccountRequestDto) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Onboard trading account request received for {} by {}", tradingAccountRequestDto.getUserId(), userDetails.getUsername());
        TradingAccountResponseDto tradingAccountResponseDto = tradingAccountService.onboardTradingAccount(userDetails, tradingAccountRequestDto);
        RestResponse<TradingAccountResponseDto> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), tradingAccountResponseDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{accountUserId}")
    public ResponseEntity<Object> updateTradingAccount(@PathVariable String accountUserId,
                                                    @Valid @RequestBody UpdateTradingAccountRequestDto updateTradingAccountRequestDto) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Modify trading account request received for {} by {}", accountUserId, userDetails.getUsername());
        TradingAccountResponseDto tradingAccountResponseDto = tradingAccountService.updateTradingAccount(userDetails, accountUserId, updateTradingAccountRequestDto);
        RestResponse<TradingAccountResponseDto> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), tradingAccountResponseDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{accountUserId}/unmap")
    public ResponseEntity<Object> unmapTradingAccount(@PathVariable String accountUserId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Temporarily Unmapping trading account request received for {} by {}", accountUserId, userDetails.getUsername());
        TradingAccountResponseDto tradingAccountResponseDto = tradingAccountService.mapUnmapTradingAccount(userDetails, accountUserId, true);
        RestResponse<TradingAccountResponseDto> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, "Trading account unmapped successfully",
                String.valueOf(HttpStatus.OK.value()), tradingAccountResponseDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{accountUserId}/map")
    public ResponseEntity<Object> mapTradingAccount(@PathVariable String accountUserId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Mapping account request received for {} by {}", accountUserId, userDetails.getUsername());
        TradingAccountResponseDto tradingAccountResponseDto = tradingAccountService.mapUnmapTradingAccount(userDetails, accountUserId, false);
        RestResponse<TradingAccountResponseDto> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, "Trading account mapped successfully",
                String.valueOf(HttpStatus.OK.value()), tradingAccountResponseDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{accountUserId}")
    public ResponseEntity<Object> deleteTradingAccount(@PathVariable String accountUserId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Delete trading account request received for {} by {}", accountUserId, userDetails.getUsername());
        tradingAccountService.deleteTradingAccount(userDetails, accountUserId);
        RestResponse<TradingAccountResponseDto> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, "Trading account deleted successfully",
                String.valueOf(HttpStatus.OK.value()), null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{tradingAccountUserId}")
    public ResponseEntity<Object> getTradingAccount(@PathVariable String tradingAccountUserId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
       TradingAccountResponseDto tradingAccountResponseDto = tradingAccountService.getTradingAccountDto(userDetails, tradingAccountUserId);
        RestResponse<TradingAccountResponseDto> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), tradingAccountResponseDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/profiles")
    public ResponseEntity<Object> getProfiles() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        List<UserProfile> profiles = tradingAccountService.getTradingAccountProfiles(userDetails);
        RestResponse<List<UserProfile>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), profiles);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/renew-request-tokens")
    public ResponseEntity<Object> renewRequestTokens() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        String message = tokenService.renewRequestTokens(userDetails, false);
        RestResponse<String> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), message);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{tradingAccountUserId}/totp")
    public ResponseEntity<Object> getTotp(@PathVariable String tradingAccountUserId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Get TOTP request received for {} by {}", tradingAccountUserId, userDetails.getUsername());
        String totp = tradingAccountService.getTotp(userDetails, tradingAccountUserId);
        RestResponse<String> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), totp);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
