package com.fam.vest.service;

import com.fam.vest.dto.response.GainersLosersResponse;
import com.fam.vest.dto.response.HoldingDetails;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface HoldingService {

    List<HoldingDetails> getAllHoldings();

    List<HoldingDetails> getHoldings(UserDetails userDetails, Optional<String> type, Optional<String> userId);

    GainersLosersResponse getGainersAndLosers(UserDetails userDetails, String timeframe, Optional<List<String>> userIds);

    void generateAndNotifyWeeklyPortfolioReport();

    void generateAndNotifyMonthlyPortfolioReport();

    void generateAndNotifyQuarterlyPortfolioReport();

    void generateAndNotifyYearlyPortfolioReport();
}
