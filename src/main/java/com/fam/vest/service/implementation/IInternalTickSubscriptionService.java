package com.fam.vest.service.implementation;

import com.fam.vest.config.InternalKiteWebSocketConnector;
import com.fam.vest.service.InternalTickSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Set;

@Slf4j
@Service
public class IInternalTickSubscriptionService implements InternalTickSubscriptionService {

    private final InternalKiteWebSocketConnector internalKiteWebSocketConnector;

    @Autowired
    public IInternalTickSubscriptionService(InternalKiteWebSocketConnector internalKiteWebSocketConnector) {
        this.internalKiteWebSocketConnector = internalKiteWebSocketConnector;
    }

    @Override
    public synchronized void subscribeToKiteWebsocket(Set<Long> instrumentTokens) {
        internalKiteWebSocketConnector.subscribeWebsocketForInternalInstruments(instrumentTokens);
    }

}
