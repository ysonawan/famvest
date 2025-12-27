package com.fam.vest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fam.vest.pojo.OrderUpdate;
import com.fam.vest.pojo.email.ResendWebhookEvent;
import com.fam.vest.service.IpoService;
import com.fam.vest.service.WebSocketFeedService;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/rest/public")
public class PublicController {

    private final ObjectMapper objectMapper;
    private final WebSocketFeedService webSocketFeedService;
    private final IpoService ipoService;

    @PostMapping(path = "/order-update/{accountId}", consumes = "application/x-www-form-urlencoded")
    public void orderUpdate(HttpServletRequest request, @PathVariable String accountId) {
        String rawRequestBody = null;
        try {
            rawRequestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            String postbackString = URLDecoder.decode(rawRequestBody, StandardCharsets.UTF_8);
            OrderUpdate orderUpdate = objectMapper.readValue(postbackString, OrderUpdate.class);
            log.info("Postback for order update: {} received for: {}", orderUpdate, accountId);
            webSocketFeedService.feedOrderUpdates(orderUpdate);
        } catch (IOException e) {
            log.error("Error processing order update postback: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/resend/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody ResendWebhookEvent event) {
        log.debug("Received webhook: Type = {}, Data = {}", event.getType(), event.getData());
        switch (event.getType()) {
            case "email.delivered":
                log.debug("Email delivered: {}", event.getData());
                break;
            case "email.bounced":
                log.warn("Email bounced: {}", event.getData());
                break;
            case "email.complained":
                log.warn("Complaint received: {}", event.getData());
                break;
            case "email.sent":
                log.debug("Email sent: {}", event.getData());
                break;
            case "email.clicked":
                log.debug("Email clicked: {}", event.getData());
                break;
            default:
                log.info("Unhandled event type: {}", event.getData());
        }
        return ResponseEntity.ok().build();
    }
}
