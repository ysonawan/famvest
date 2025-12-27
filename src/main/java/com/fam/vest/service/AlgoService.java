package com.fam.vest.service;

import com.fam.vest.dto.request.StatusUpdateRequest;
import com.fam.vest.dto.request.StraddleStrategyRequest;
import com.fam.vest.entity.StraddleStrategy;
import com.fam.vest.entity.StraddleStrategyExecution;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface AlgoService {

    List<StraddleStrategy> getStraddleStrategies(UserDetails userDetails, Optional<Boolean> active);

    List<StraddleStrategyExecution> getStraddleStrategyExecutions(UserDetails userDetails);

    List<StraddleStrategyExecution> getStraddleStrategyExecutionsByStraddleId(UserDetails userDetails, Long straddleId);

    void deleteStraddleStrategy(Long id, UserDetails userDetails);

    StraddleStrategy saveStraddleStrategy(UserDetails userDetails, StraddleStrategyRequest straddleStrategyRequest);

    StraddleStrategy updateStraddleStrategy(UserDetails userDetails, Long id, StraddleStrategyRequest straddleStrategyRequest);

    StraddleStrategy getStraddleStrategy(UserDetails userDetails, Long id);

    StraddleStrategy updateStraddleStrategyStatus(UserDetails userDetails, Long id, StatusUpdateRequest statusUpdateRequest);

    StraddleStrategy executeStraddleStrategy(UserDetails userDetails, Long id);
}
