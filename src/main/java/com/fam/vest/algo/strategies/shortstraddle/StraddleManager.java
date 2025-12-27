package com.fam.vest.algo.strategies.shortstraddle;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.entity.StraddleStrategy;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.pojo.ExchangeTimingResponse;
import com.fam.vest.repository.InstrumentRepository;
import com.fam.vest.repository.StraddleStrategyExecutionRepository;
import com.fam.vest.repository.StraddleStrategyRepository;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.EmailService;
import com.fam.vest.service.QuoteService;
import com.fam.vest.service.MarketInformationService;
import com.zerodhatech.kiteconnect.KiteConnect;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class StraddleManager {

    private static final org.slf4j.Logger straddleLogger = LoggerFactory.getLogger("ALGO_STRADDLE_LOGGER");

    private final StraddleStrategyRepository straddleStrategyRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final KiteConnector kiteConnector;
    private final QuoteService quoteService;
    private final InstrumentRepository instrumentRepository;
    private final MarketInformationService marketInformationService;
    private final StraddleStrategyExecutionRepository straddleStrategyExecutionRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private ScheduledExecutorService scheduler;


    @Autowired
    @Qualifier("straddleScheduler")
    private ScheduledExecutorService taskScheduler;

    @Autowired
    @Qualifier("straddleExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void init() {
        if (isTradingHoliday()) return;
        int delay = 60;
        straddleLogger.info("Schedule straddle manager will start in {} seconds after startup", delay);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                this.scheduleStraddles();
            } catch (Exception e) {
                straddleLogger.error("Error in schedule straddle manager: {}", e.getMessage(), e);
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void scheduleStraddles() {
        straddleLogger.info("Straddles scheduler activity started");
        List<StraddleStrategy> straddleStrategyList = straddleStrategyRepository.findAll();
        straddleLogger.info("{} straddle strategies are found", straddleStrategyList.size());
        for (StraddleStrategy straddleStrategy : straddleStrategyList) {
            straddleLogger.info("[{}] Processing straddle strategy: {}", straddleStrategy.getUserId(), straddleStrategy);
            if(!straddleStrategy.getIsActive()) {
                straddleLogger.info("[{}] [{}] Straddle strategy is not active. Skipping", straddleStrategy.getUserId(), straddleStrategy.getInstrument());
                continue;
            }
            TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(straddleStrategy.getUserId());
            if(null != tradingAccount) {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime entryTime = LocalDateTime.of(LocalDate.now(), straddleStrategy.getEntryTime().toLocalTime());
                long delay = Duration.between(now, entryTime).toMillis();
                if(delay < 0) {
                    straddleLogger.info("[{}] [{}] Straddle entry time {} already passed. Skipping", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), entryTime);
                    continue;
                }
                straddleLogger.info("[{}] [{}] Scheduling straddle strategy to run at {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), entryTime);
                taskScheduler.schedule(() -> {
                    taskExecutor.execute(new StraddleTask(kiteConnect, straddleStrategy.getId(), quoteService,
                            instrumentRepository, straddleStrategyRepository,
                            straddleStrategyExecutionRepository, emailService, templateEngine));
                }, delay, TimeUnit.MILLISECONDS);
            } else {
                straddleLogger.warn("[{}] [{}] Trading account for {} does not exist", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getUserId());
            }
        }
    }

    public void executeStraddleStrategy(StraddleStrategy straddleStrategy) {
        if (isTradingHoliday()) throw new InternalException("Today is trading holiday. Straddle can not be executed.");
        straddleLogger.info("[{}] [Invoked] Executing straddle strategy: {}", straddleStrategy.getUserId(), straddleStrategy);
        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(straddleStrategy.getUserId());
        if (tradingAccount != null) {
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            taskExecutor.execute(new StraddleTask(kiteConnect, straddleStrategy.getId(), quoteService,
                    instrumentRepository, straddleStrategyRepository,
                    straddleStrategyExecutionRepository, emailService, templateEngine));
        } else {
            straddleLogger.warn("[{}] [{}] [Invoked] Trading account for user {} does not exist", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getUserId());
        }
    }

    private boolean isTradingHoliday() {
        LocalDate now = LocalDate.now();
        ExchangeTimingResponse exchangeTimingResponse = marketInformationService.getExchangeTradingTime(now);
        if(null != exchangeTimingResponse.getData() && exchangeTimingResponse.getData().isEmpty()) {
            straddleLogger.info("{} is trading holiday. Skipping straddle strategy execution.", now);
            return true;
        }
        return false;
    }

    @PreDestroy
    public void shutdown() {
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
