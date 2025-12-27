package com.fam.vest.service.implementation;

import com.fam.vest.algo.strategies.shortstraddle.StraddleManager;
import com.fam.vest.dto.request.StatusUpdateRequest;
import com.fam.vest.dto.request.StraddleStrategyRequest;
import com.fam.vest.entity.StraddleStrategy;
import com.fam.vest.entity.StraddleStrategyExecution;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.repository.StraddleStrategyExecutionRepository;
import com.fam.vest.repository.StraddleStrategyRepository;
import com.fam.vest.service.AlgoService;
import com.fam.vest.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IAlgoService implements AlgoService {

    private final StraddleStrategyRepository straddleStrategyRepository;
    private final StraddleManager straddleManager;
    private final StraddleStrategyExecutionRepository straddleStrategyExecutionRepository;

    @Override
    public List<StraddleStrategy> getStraddleStrategies(UserDetails userDetails, Optional<Boolean> active) {
        List<StraddleStrategy> straddleStrategies = straddleStrategyRepository.findStraddleStrategiesByCreatedByOrderByCreatedDate(userDetails.getUsername());
        return active.map(aBoolean -> straddleStrategies.stream().filter(straddleStrategy -> straddleStrategy.getIsActive().equals(aBoolean)).toList()).orElse(straddleStrategies);
    }

    @Override
    public List<StraddleStrategyExecution> getStraddleStrategyExecutions(UserDetails userDetails) {
        return straddleStrategyExecutionRepository.findAllWithStrategyByCreatedBy(userDetails.getUsername());
    }

    @Override
    public List<StraddleStrategyExecution> getStraddleStrategyExecutionsByStraddleId(UserDetails userDetails, Long straddleId) {
        return straddleStrategyExecutionRepository.findAllWithStrategyByCreatedByAndStrategyId(userDetails.getUsername(), straddleId);
    }

    @Override
    public void deleteStraddleStrategy(Long id, UserDetails userDetails) {
        StraddleStrategy existing = straddleStrategyRepository.findStraddleStrategiesByCreatedByAndId(userDetails.getUsername(), id).orElse(null);
        if(existing == null) {
            throw new ResourceNotFoundException("Straddle strategy with id " + id + " not found");
        }
        straddleStrategyRepository.delete(existing);
     }

    @Override
    public StraddleStrategy saveStraddleStrategy(UserDetails userDetails, StraddleStrategyRequest straddleStrategyRequest) {
        Date currentDate = Calendar.getInstance().getTime();
        StraddleStrategy straddleStrategy = new StraddleStrategy();
        straddleStrategy.setIsActive(true);
        straddleStrategy.setTrailingSl(false);
        straddleStrategy.setInstrument(straddleStrategyRequest.getInstrument());
        straddleStrategy.setSide(straddleStrategyRequest.getSide());
        straddleStrategy.setIndex(straddleStrategyRequest.getIndex());
        straddleStrategy.setExchange(straddleStrategyRequest.getExchange());
        straddleStrategy.setTradingSegment(straddleStrategyRequest.getTradingSegment());
        straddleStrategy.setUnderlyingStrikeSelector(straddleStrategyRequest.getUnderlyingStrikeSelector());
        straddleStrategy.setUnderlyingSegment(straddleStrategyRequest.getUnderlyingSegment());
        straddleStrategy.setUserId(straddleStrategyRequest.getUserId());
        straddleStrategy.setLots(straddleStrategyRequest.getLots());
        ZoneId inputZone = ZoneId.of("Asia/Kolkata");
        straddleStrategy.setEntryTime(CommonUtil.convertToUtcTime(straddleStrategyRequest.getEntryTime().toLocalTime(), inputZone));
        straddleStrategy.setExitTime(CommonUtil.convertToUtcTime(straddleStrategyRequest.getExitTime().toLocalTime(), inputZone));
        if (straddleStrategy.getEntryTime().compareTo(straddleStrategy.getExitTime()) >= 0) {
            throw new IllegalArgumentException("Entry time must be before exit time");
        }
        straddleStrategy.setExpiryScope(straddleStrategyRequest.getExpiryScope());
        straddleStrategy.setMarketOrder(straddleStrategyRequest.getOrderType().equals("MARKET"));
        straddleStrategy.setStrikeStep(straddleStrategyRequest.getStrikeStep());
        straddleStrategy.setPaperTrade(straddleStrategyRequest.getTradeType().equals("PAPER"));
        straddleStrategy.setTrailingSl(straddleStrategyRequest.getTrailingSl().equals("Yes"));
        straddleStrategy.setTarget(straddleStrategyRequest.getTarget());
        straddleStrategy.setStopLoss(straddleStrategyRequest.getStopLoss());
        straddleStrategy.setCreatedBy(userDetails.getUsername());
        straddleStrategy.setCreatedDate(currentDate);
        straddleStrategy.setLastModifiedBy(userDetails.getUsername());
        straddleStrategy.setLastModifiedDate(currentDate);
        return straddleStrategyRepository.save(straddleStrategy);
    }

    @Override
    public StraddleStrategy updateStraddleStrategy(UserDetails userDetails, Long id, StraddleStrategyRequest straddleStrategyRequest) {
        Date currentDate = Calendar.getInstance().getTime();
        StraddleStrategy existing = straddleStrategyRepository.findStraddleStrategiesByCreatedByAndId(userDetails.getUsername(), id).orElse(null);
        if(existing == null) {
            throw new ResourceNotFoundException("Straddle strategy with id " + id + " not found");
        }
        existing.setSide(straddleStrategyRequest.getSide());
        existing.setLots(straddleStrategyRequest.getLots());
        ZoneId inputZone = ZoneId.of("Asia/Kolkata");
        existing.setEntryTime(CommonUtil.convertToUtcTime(straddleStrategyRequest.getEntryTime().toLocalTime(), inputZone));
        existing.setExitTime(CommonUtil.convertToUtcTime(straddleStrategyRequest.getExitTime().toLocalTime(), inputZone));
        if (existing.getEntryTime().compareTo(existing.getExitTime()) >= 0) {
            throw new IllegalArgumentException("Entry time must be before exit time");
        }
        existing.setExpiryScope(straddleStrategyRequest.getExpiryScope());
        existing.setUnderlyingStrikeSelector(straddleStrategyRequest.getUnderlyingStrikeSelector());
        existing.setUnderlyingSegment(straddleStrategyRequest.getUnderlyingSegment());
        existing.setMarketOrder(straddleStrategyRequest.getOrderType().equals("MARKET"));
        existing.setStrikeStep(straddleStrategyRequest.getStrikeStep());
        existing.setPaperTrade(straddleStrategyRequest.getTradeType().equals("PAPER"));
        existing.setTrailingSl(straddleStrategyRequest.getTrailingSl().equals("Yes"));
        existing.setTarget(straddleStrategyRequest.getTarget());
        existing.setStopLoss(straddleStrategyRequest.getStopLoss());
        existing.setLastModifiedBy(userDetails.getUsername());
        existing.setLastModifiedDate(currentDate);
        return straddleStrategyRepository.save(existing);
    }


    @Override
    public StraddleStrategy getStraddleStrategy(UserDetails userDetails, Long id) {
        Optional<StraddleStrategy> straddleStrategy = straddleStrategyRepository.findStraddleStrategiesByCreatedByAndId(userDetails.getUsername(), id);
        return straddleStrategy.orElseThrow(() -> new ResourceNotFoundException("Straddle strategy not found for id: " + id));
    }

    @Override
    public StraddleStrategy updateStraddleStrategyStatus(UserDetails userDetails, Long id, StatusUpdateRequest statusUpdateRequest) {
        Optional<StraddleStrategy> straddleStrategy = straddleStrategyRepository.findStraddleStrategiesByCreatedByAndId(userDetails.getUsername(), id);
        if(straddleStrategy.isEmpty()) {
            throw new ResourceNotFoundException("Straddle strategy not found for id: " + id);
        }
        straddleStrategy.get().setIsActive(statusUpdateRequest.getStatus().equalsIgnoreCase("ACTIVATE"));
        return straddleStrategyRepository.save(straddleStrategy.get());
    }

    @Override
    public StraddleStrategy executeStraddleStrategy(UserDetails userDetails, Long id) {
        Optional<StraddleStrategy> straddleStrategy = straddleStrategyRepository.findStraddleStrategiesByCreatedByAndId(userDetails.getUsername(), id);
        if(straddleStrategy.isPresent()) {
            straddleManager.executeStraddleStrategy(straddleStrategy.get());
        } else {
            throw new ResourceNotFoundException("Straddle strategy not found for id: " + id);
        }
        return straddleStrategy.get();
    }
}
