package com.fam.vest.service;

import com.zerodhatech.models.Tick;

import java.util.List;

public interface InternalTickFeedService {

    Tick getLatestTick(Long instrumentToken);

    void feedTicks(List<Tick> ticks);
}
