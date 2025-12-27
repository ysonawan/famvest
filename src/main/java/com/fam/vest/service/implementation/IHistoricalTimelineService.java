package com.fam.vest.service.implementation;

import com.fam.vest.entity.HistoricalTimelineValues;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.repository.HistoricalTimelineValuesRepository;
import com.fam.vest.service.HistoricalTimelineService;
import com.fam.vest.service.TradingAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IHistoricalTimelineService implements HistoricalTimelineService {

    private final TradingAccountService tradingAccountService;
    private final HistoricalTimelineValuesRepository historicalTimelineValuesRepository;

    public IHistoricalTimelineService(TradingAccountService tradingAccountService,
                                      HistoricalTimelineValuesRepository historicalTimelineValuesRepository) {
        this.tradingAccountService = tradingAccountService;
        this.historicalTimelineValuesRepository = historicalTimelineValuesRepository;
    }

    @Override
    public List<HistoricalTimelineValues> getHistoricalTimelineValues(UserDetails userDetails, Optional<String> tradingAccountId,
                                                                      String type) {
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
        List<String> targetUserIds = tradingAccounts.stream().map(TradingAccount::getUserId).toList();
        List<HistoricalTimelineValues> historicalTimelineValues = historicalTimelineValuesRepository.findAllByOrderByDate();
        historicalTimelineValues = this.filterByUserId(historicalTimelineValues, targetUserIds);
        this.nullifyTimelineFieldsByType(historicalTimelineValues, type);
        return historicalTimelineValues;
    }

    private void nullifyTimelineFieldsByType(List<HistoricalTimelineValues> historicalTimelineValues, String type) {
        if(type.equalsIgnoreCase("funds")) {
            historicalTimelineValues.forEach(htv -> htv.setHistoricalMfSipsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalHoldingsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalPositionsTimelines(null));
        } else if(type.equalsIgnoreCase("sips")) {
            historicalTimelineValues.forEach(htv -> htv.setHistoricalFundsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalHoldingsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalPositionsTimelines(null));
        } else if(type.equalsIgnoreCase("holdings")) {
            historicalTimelineValues.forEach(htv -> htv.setHistoricalFundsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalMfSipsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalPositionsTimelines(null));
        } else if(type.equalsIgnoreCase("positions")) {
            historicalTimelineValues.forEach(htv -> htv.setHistoricalFundsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalMfSipsTimelines(null));
            historicalTimelineValues.forEach(htv -> htv.setHistoricalHoldingsTimelines(null));
        }
    }

    public List<HistoricalTimelineValues> filterByUserId(List<HistoricalTimelineValues> sourceList, List<String> targetUserIds) {
        List<HistoricalTimelineValues> filteredList = new ArrayList<>();
        sourceList.forEach(source -> {
            HistoricalTimelineValues filtered = new HistoricalTimelineValues();
            filtered.setId(source.getId());
            filtered.setDate(source.getDate());
            filtered.setCreatedDate(source.getCreatedDate());
            filtered.setLastModifiedDate(source.getLastModifiedDate());
            if(null != source.getHistoricalFundsTimelines()) {
                filtered.setHistoricalFundsTimelines(source.getHistoricalFundsTimelines().stream()
                                .filter(f -> targetUserIds.contains(f.getUserId())).toList()
                );
            }
           if(null != source.getHistoricalMfSipsTimelines()) {
               filtered.setHistoricalMfSipsTimelines(source.getHistoricalMfSipsTimelines().stream()
                               .filter(f -> targetUserIds.contains(f.getUserId())).toList()
               );
           }
           if(null != source.getHistoricalHoldingsTimelines()) {
               filtered.setHistoricalHoldingsTimelines(source.getHistoricalHoldingsTimelines().stream()
                               .filter(f -> targetUserIds.contains(f.getUserId())).toList()
               );
           }
           if(null != source.getHistoricalPositionsTimelines()) {
               filtered.setHistoricalPositionsTimelines(source.getHistoricalPositionsTimelines().stream()
                               .filter(f -> targetUserIds.contains(f.getUserId())).toList()
               );
           }
            filteredList.add(filtered);
        });

        return filteredList;
    }

}
