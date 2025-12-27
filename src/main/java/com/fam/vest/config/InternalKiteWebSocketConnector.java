package com.fam.vest.config;

import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.InternalTickFeedService;
import com.fam.vest.service.TokenService;
import com.zerodhatech.models.Tick;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InternalKiteWebSocketConnector extends BaseKiteWebSocketConnector {

    private final InternalTickFeedService internalTickFeedService;
    private final Set<Long> internalSubscribedInstrumentTokens = new HashSet<>();

    public InternalKiteWebSocketConnector(KiteConnector kiteConnector,
                                          TradingAccountRepository tradingAccountRepository,
                                          TokenService tokenService,
                                          InternalTickFeedService internalTickFeedService) {
        super(kiteConnector, tradingAccountRepository, tokenService);
        this.internalTickFeedService = internalTickFeedService;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing kite web socket connector for internal");
        int delay = 60;
        log.info("Scheduling kite web socket connector for internal {} seconds after startup", delay);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                subscribeKiteWebsocket();
            } catch (Exception e) {
                log.error("Error while initializing kite web socket connector for internal: {}", e.getMessage(), e);
            }
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    protected void handleTicks(ArrayList<Tick> ticks) {
        log.debug("Feeding {} tick for internal tick feed service", ticks.size());
        List<Tick> subscribedTicks = ticks.stream()
                .filter(tick -> internalSubscribedInstrumentTokens.contains(tick.getInstrumentToken())).toList();
        internalTickFeedService.feedTicks(subscribedTicks);
    }

    public void subscribeWebsocketForInternalInstruments(Set<Long> tokens) {
        if(null != tokens && !tokens.isEmpty()) {
            /*tokens = tokens.stream().filter(t -> !this.internalSubscribedInstrumentTokens.contains(t))
                    .collect(Collectors.toSet());*/
            this.internalSubscribedInstrumentTokens.addAll(tokens);
            this.subscribeWebsocket(tokens);
        }
    }
}
