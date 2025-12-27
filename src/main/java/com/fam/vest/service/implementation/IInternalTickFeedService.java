package com.fam.vest.service.implementation;

import com.fam.vest.service.InternalTickFeedService;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class IInternalTickFeedService implements InternalTickFeedService {

    // Store latest tick per instrument
    private final ConcurrentMap<Long, Tick> latestTicks = new ConcurrentHashMap<>();

    @Override
    public Tick getLatestTick(Long instrument) {
        return latestTicks.get(instrument);
    }

    @Override
    public void feedTicks(List<Tick> ticks) {
        ticks.forEach(tick -> {
            latestTicks.put(tick.getInstrumentToken(), tick);
        });
    }
}
