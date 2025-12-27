package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.dto.request.CombinedMarginCalculationRequest;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.dto.response.FundDetails;
import com.fam.vest.dto.request.MarginCalculationRequest;
import com.fam.vest.service.FundsService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class IFundsService implements FundsService {

    private final TradingAccountService tradingAccountService;
    private final KiteConnector kiteConnector;

    @Autowired
    public IFundsService(TradingAccountService tradingAccountService,
                         KiteConnector kiteConnector) {
        this.tradingAccountService = tradingAccountService;
        this.kiteConnector = kiteConnector;
    }

    @Override
    public List<FundDetails> getAllFunds() {
        return this.getFunds(null, Optional.ofNullable(null));
    }

    @Override
    public List<FundDetails> getFunds(UserDetails userDetails, Optional<String> tradingAccountId) {
        List<TradingAccount> tradingAccounts = null;
        if(null == userDetails) {
            tradingAccounts = tradingAccountService.getAllTradingAccounts();
        } else {
            tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        }
        if(tradingAccountId.isPresent()) {
            tradingAccounts = tradingAccounts.stream().
                    filter(tradingAccount -> tradingAccount.getUserId().equals(tradingAccountId.get())).toList();
        }
        List<FundDetails> fundDetails = new ArrayList<>();
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                this.getFunds(kiteConnect, fundDetails);
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting funds for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return fundDetails;
    }

    private void getFunds(KiteConnect kiteConnect, List<FundDetails> fundDetails) throws KiteException, IOException {
        Map<String, Margin> margins = kiteConnect.getMargins();
        FundDetails fundDetail = new FundDetails();
        fundDetail.setUserId(kiteConnect.getUserId());
        fundDetail.setMargin(margins.get("equity"));
        fundDetails.add(fundDetail);
    }

    @Override
    public List<MarginCalculationData> getMarginCalculation(UserDetails userDetails, MarginCalculationRequest marginCalculationRequest) {
        List<MarginCalculationData> marginCalculation = new ArrayList<>();
        TradingAccount tradingAccount = tradingAccountService.getTradingAccount(userDetails, marginCalculationRequest.getTradingAccountId());
        try {
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            MarginCalculationParams marginCalculationParams = marginCalculationRequest.getMarginCalculationParams();
            List<MarginCalculationParams> params = new ArrayList<>();
            params.add(marginCalculationParams);
            marginCalculation = kiteConnect.getMarginCalculation(params);
        } catch (RequestTokenMissingException e) {
            log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while getting margin calculation for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return marginCalculation;
    }

    @Override
    public CombinedMarginData getCombinedMarginCalculation(UserDetails userDetails, CombinedMarginCalculationRequest combinedMarginCalculationRequest) {
        CombinedMarginData combinedMarginData = null;
        TradingAccount tradingAccount = tradingAccountService.getTradingAccount(userDetails, combinedMarginCalculationRequest.getTradingAccountId());
        try {
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            combinedMarginData = kiteConnect.getCombinedMarginCalculation(combinedMarginCalculationRequest.getMarginCalculationParams(),
                    combinedMarginCalculationRequest.isIncludeExistingPositions(), false);
        } catch (RequestTokenMissingException e) {
            log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while getting combined margin calculation for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return combinedMarginData;
    }

}
