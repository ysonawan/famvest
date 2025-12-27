package com.fam.vest.service;


import com.fam.vest.entity.HistoricalTimelineValues;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface HistoricalTimelineService {

    List<HistoricalTimelineValues> getHistoricalTimelineValues(UserDetails userDetails, Optional<String> tradingAccountId, String type);
}
