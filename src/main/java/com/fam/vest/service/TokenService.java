package com.fam.vest.service;

import com.fam.vest.entity.TradingAccount;
import org.springframework.security.core.userdetails.UserDetails;

public interface TokenService {

    void renewRequestToken(String tradingAccountUserId);
    String renewRequestTokens(UserDetails userDetails, boolean isOnApplicationStart);
    String getENCToken(TradingAccount tradingAccount);
    String getTotp(String totpKey);
}
