package com.fam.vest.service;

import com.fam.vest.dto.response.HoldingDetails;
import com.fam.vest.dto.response.MFOrderDetails;
import com.fam.vest.dto.response.MFSIPDetails;
import com.fam.vest.dto.request.SIPRequest;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public interface MutualFundService {

    List<HoldingDetails> getMutualFundHoldings(UserDetails userDetails, Optional<String> tradingAccountId);

    void getMutualFundHoldings(String userId, KiteConnect kiteConnect, List<HoldingDetails> holdingDetails, AtomicLong sequenceNumber)
            throws KiteException, IOException;

    List<MFOrderDetails> getAllMutualFundOrders();

    List<MFOrderDetails> getConsolidatedMutualFundOrders(UserDetails userDetails, Optional<String> tradingAccountId);

    List<MFSIPDetails> updateSip(String sipId, String tradingAccountId, UserDetails userDetails, SIPRequest sipRequest);

    List<MFSIPDetails> getAllMutualFundSips();

    List<MFSIPDetails> getMutualFundSips(UserDetails userDetails, Optional<String> tradingAccountId);

    void generateAndNotifyMonthlySipReport();
}
