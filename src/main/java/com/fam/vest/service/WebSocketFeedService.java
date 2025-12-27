package com.fam.vest.service;

import com.fam.vest.pojo.OrderUpdate;
import com.zerodhatech.models.Tick;

import java.util.List;

public interface WebSocketFeedService {

    void feedTicks(List<Tick> ticks);

    void feedOrderUpdates(OrderUpdate orderUpdate);
}
