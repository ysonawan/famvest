package com.fam.vest.controller;

import com.fam.vest.config.ClientSessionRegistry;
import com.fam.vest.config.FontEndKiteWebSocketConnector;
import com.fam.vest.dto.request.SubscriptionRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Set;

@Slf4j
@Controller
public class WebSocketSubscriptionController {

    private final ClientSessionRegistry registry;
    private final FontEndKiteWebSocketConnector fontEndKiteWebSocketConnector;

    @Autowired
    public WebSocketSubscriptionController(ClientSessionRegistry registry,
                                           FontEndKiteWebSocketConnector fontEndKiteWebSocketConnector) {
        this.registry = registry;
        this.fontEndKiteWebSocketConnector = fontEndKiteWebSocketConnector;
    }

    @MessageMapping("/subscribe")
    public void subscribe(@Payload @Valid SubscriptionRequest request,
                          SimpMessageHeaderAccessor headers,
                          Principal principal) {
        String sessionId = headers.getSessionId();
        String userName = principal.getName();

        registry.addTokens(userName, sessionId, request.instrumentTokens());
        log.debug("[{}] with session [{}] subscribed to {} in FamVest app web socket", userName, sessionId, request.instrumentTokens());

        fontEndKiteWebSocketConnector.subscribeWebsocket(request.instrumentTokens());
        log.debug("[{}] with session [{}] subscribed to {} in kite web socket", userName, sessionId, request.instrumentTokens());
    }

    @MessageMapping("/unsubscribe")
    public void unsubscribe(@Payload @Valid SubscriptionRequest request,
                            SimpMessageHeaderAccessor headers,
                            Principal principal) {
        String sessionId = headers.getSessionId();
        String userName = principal.getName();

        Set<Long> tokensExclusivelyForSession = registry.tokensExclusivelyForSession(sessionId, request.instrumentTokens());
        fontEndKiteWebSocketConnector.unsubscribeWebsocket(tokensExclusivelyForSession);
        log.debug("[{}] with session [{}] unsubscribed to {} in kite web socket", userName, sessionId, request.instrumentTokens());

        registry.removeTokens(sessionId, request.instrumentTokens());
        log.debug("[{}] with session [{}] unsubscribed to {} in FamVest app web socket", userName, sessionId, request.instrumentTokens());
    }

    // Auto‑cleanup when browser closes or WS disconnects
    @EventListener
    public void onDisconnect(SessionDisconnectEvent e) {
        Set<Long> instrumentTokens = registry.tokensForSessionId(e.getSessionId());
        if(null != instrumentTokens && !instrumentTokens.isEmpty()) {
            Set<Long> tokensExclusivelyForSession = registry.tokensExclusivelyForSession(e.getSessionId(), instrumentTokens);
            fontEndKiteWebSocketConnector.unsubscribeWebsocket(tokensExclusivelyForSession);
            log.debug("onDisconnect: session [{}] unsubscribed to {} in kite web socket", e.getSessionId(), tokensExclusivelyForSession);

            }
        registry.removeSession(e.getSessionId());
        log.debug("onDisconnect: [{}] disconnected to FamVest app web socket", e.getSessionId());
    }
}
