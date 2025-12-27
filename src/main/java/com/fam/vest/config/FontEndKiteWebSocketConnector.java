package com.fam.vest.config;

import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.TokenService;
import com.fam.vest.service.WebSocketFeedService;
import com.zerodhatech.models.Tick;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FontEndKiteWebSocketConnector extends BaseKiteWebSocketConnector {

    private final WebSocketFeedService websocketFeedService;

    public FontEndKiteWebSocketConnector(KiteConnector kiteConnector,
                                         TradingAccountRepository tradingAccountRepository,
                                         TokenService tokenService,
                                         WebSocketFeedService websocketFeedService) {
        super(kiteConnector, tradingAccountRepository, tokenService);
        this.websocketFeedService = websocketFeedService;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing kite web socket connector for front end");
        int delay = 40;
        log.info("Scheduling kite web socket connector for front end {} seconds after startup", delay);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                subscribeKiteWebsocket();
            } catch (Exception e) {
                log.error("Error while initializing kite web socket connector for front end: {}", e.getMessage(), e);
            }
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    protected void handleTicks(ArrayList<Tick> ticks) {
        log.debug("Feeding {} tick for front end tick feed service", ticks.size());
        websocketFeedService.feedTicks(ticks);
    }
}
