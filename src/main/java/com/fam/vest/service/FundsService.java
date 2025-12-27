package com.fam.vest.service;

import com.fam.vest.dto.request.CombinedMarginCalculationRequest;
import com.fam.vest.dto.response.FundDetails;
import com.fam.vest.dto.request.MarginCalculationRequest;
import com.zerodhatech.models.CombinedMarginData;
import com.zerodhatech.models.MarginCalculationData;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface FundsService {

    List<FundDetails> getAllFunds();

    List<FundDetails> getFunds(UserDetails userDetails, Optional<String> userId);

    List<MarginCalculationData> getMarginCalculation(UserDetails userDetails, MarginCalculationRequest marginCalculationRequest);

    CombinedMarginData getCombinedMarginCalculation(UserDetails userDetails, CombinedMarginCalculationRequest combinedMarginCalculationRequest);

}
