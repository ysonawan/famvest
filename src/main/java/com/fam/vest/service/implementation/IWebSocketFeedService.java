package com.fam.vest.service.implementation;

import com.fam.vest.config.ClientSessionRegistry;
import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.pojo.OrderUpdate;
import com.fam.vest.repository.ApplicationUserRepository;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.service.WebSocketFeedService;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class IWebSocketFeedService implements WebSocketFeedService {

    private final ClientSessionRegistry registry;
    private final SimpMessagingTemplate template;
    private final TradingAccountService tradingAccountService;
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    public IWebSocketFeedService(ClientSessionRegistry registry,
                                 SimpMessagingTemplate template,
                                 ApplicationUserRepository applicationUserRepository, TradingAccountService tradingAccountService) {
        this.registry = registry;
        this.template = template;
        this.applicationUserRepository = applicationUserRepository;
        this.tradingAccountService = tradingAccountService;
    }

    @Override
    public void feedTicks(List<Tick> ticks) {
        // Loop through each session
        for (String sessionId : registry.getAllSessionIds()) {
            String userName = registry.userNameForSession(sessionId);
            Set<Long> instrumentTokens = registry.tokensForSessionId(sessionId);
            // Filter ticks for this session's instrument tokens
            List<Tick> userTicks = ticks.stream()
                    .filter(tick -> instrumentTokens.contains(tick.getInstrumentToken()))
                    .toList();
            if (!userTicks.isEmpty()) {
                log.debug("Feeding {} ticks to user: {}", userTicks.size(), userName);
                template.convertAndSendToUser(userName, "/queue/ticks", userTicks);
            }
        }
    }

    @Override
    public void feedOrderUpdates(OrderUpdate orderUpdate) {
        Set<String> users = registry.getAllUserNames();
        for (String userName : users) {
            ApplicationUser applicationUser = applicationUserRepository.findApplicationUserByUserName(userName);
            if (applicationUser == null) {
                continue; // skip invalid user
            }
            List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(applicationUser, false);
            TradingAccount matchingAccount = tradingAccounts.stream()
                    .filter(account -> account.getUserId().equalsIgnoreCase(orderUpdate.getUserId()))
                    .findFirst()
                    .orElse(null);

            if (matchingAccount == null) {
                log.info("No application user and trading account mapping found for application user: {} with trading account: {}", userName, orderUpdate.getUserId());
                continue;
            }

            if (!isValidPostbackData(orderUpdate, matchingAccount)) {
                log.warn("Checksum validation failed. Invalid postback data for order update with ID: {} for user: {}", orderUpdate.getOrderId(), userName);
                continue;
            }

            log.info("Feeding order with ID {} update for user: {}", orderUpdate.getOrderId(), userName);
            template.convertAndSendToUser(userName, "/queue/orders", orderUpdate);
        }
    }


    private boolean isValidPostbackData(OrderUpdate orderUpdate, TradingAccount tradingAccount) {
        log.info("Validating postback data with checksum for order update");
        String shaFromKite = orderUpdate.getChecksum();
        String myKey = orderUpdate.getOrderId() + orderUpdate.getOrderTimestamp() + tradingAccount.getApiSecret();
        String shaFromMe = org.apache.commons.codec.digest.DigestUtils.sha256Hex(myKey);
        return shaFromKite.equals(shaFromMe);
    }

}
