package com.fam.vest.service;

import com.fam.vest.dto.response.PositionDetails;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface PositionService {

    List<PositionDetails> getAllPositions();

    List<PositionDetails> getPositions(UserDetails userDetails, Optional<String> type, Optional<String> tradingAccountId);
}
