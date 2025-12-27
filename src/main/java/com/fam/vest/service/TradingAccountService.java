package com.fam.vest.service;

import com.fam.vest.dto.request.UpdateTradingAccountRequestDto;
import com.fam.vest.dto.response.TradingAccountResponseDto;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.dto.request.TradingAccountRequestDto;
import com.fam.vest.dto.response.UserProfile;
import com.zerodhatech.models.Profile;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface TradingAccountService {

    List<TradingAccount> getTradingAccounts(UserDetails userDetails, boolean onlyActive);

    List<TradingAccount> getTradingAccounts(ApplicationUser applicationUser, boolean onlyActive);

    List<TradingAccount> getAllTradingAccounts();

    TradingAccountResponseDto onboardTradingAccount(UserDetails userDetails, TradingAccountRequestDto tradingAccountRequestDto);

    TradingAccountResponseDto getTradingAccountDto(UserDetails userDetails, String tradingAccountId);

    TradingAccountResponseDto updateTradingAccount(UserDetails userDetails, String accountUserId, UpdateTradingAccountRequestDto updateTradingAccountRequestDto);

    TradingAccountResponseDto mapUnmapTradingAccount(UserDetails userDetails, String tradingAccountId, boolean isUnmappedRequest);

    void deleteTradingAccount(UserDetails userDetails, String userId);

    TradingAccount getTradingAccount(UserDetails userDetails, String userId);

    List<UserProfile> getTradingAccountProfiles(UserDetails userDetails);

    Profile registerRequestToken(String userId, String requestToken, String status);

    String getTotp(UserDetails userDetails, String tradingAccountUserId);
}
