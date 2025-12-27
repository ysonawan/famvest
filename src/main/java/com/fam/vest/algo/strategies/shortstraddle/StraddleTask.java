package com.fam.vest.algo.strategies.shortstraddle;

import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.StraddleStrategy;
import com.fam.vest.entity.StraddleStrategyExecution;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.pojo.email.ResendEmailPayload;
import com.fam.vest.repository.InstrumentRepository;
import com.fam.vest.repository.StraddleStrategyExecutionRepository;
import com.fam.vest.repository.StraddleStrategyRepository;
import com.fam.vest.service.EmailService;
import com.fam.vest.service.QuoteService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import com.zerodhatech.models.Position;
import com.zerodhatech.models.Quote;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class StraddleTask implements Runnable {

    private static final org.slf4j.Logger straddleLogger = LoggerFactory.getLogger("ALGO_STRADDLE_LOGGER");

    private final KiteConnect kiteConnect;
    private final Long straddleStrategyId;
    private final QuoteService quoteService;
    private final InstrumentRepository instrumentRepository;
    private final StraddleStrategyRepository straddleStrategyRepository;
    private final StraddleStrategyExecutionRepository straddleStrategyExecutionRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    private StraddleStrategyExecution execution;

    // Constants
    private static final int ORDER_PRICES_UPDATE_DELAY_SECONDS = 5;
    private static final int MONITOR_PNL_INTERVAL_SECONDS = 5;
    private static final int MONITOR_PNL_INITIAL_DELAY_SECONDS = 10;
    private static final int DEFAULT_STRIKE_STEP = 100;
    private static final double TRAILING_STOP_RATIO = 0.7;
    private static final double PEAK_THRESHOLD_PERCENTAGE = 0.10;
    private static final String FUTURE_SELECTOR = "FUTURE";
    private static final String CURRENT_EXPIRY = "CURRENT";
    private static final String CALL_OPTION = "CE";
    private static final String PUT_OPTION = "PE";

    @Getter
    private enum SIDE {
        LONG("LONG"), SHORT("SHORT");

        private final String value;

        SIDE(String value) {
            this.value = value;
        }
    }

    private record StraddleInstruments(Instrument call, Instrument put) { }

    private record StraddleQuotes(Quote call, Quote put) {
        public boolean isValid() {
            return call != null && put != null;
        }
    }

    private record StraddleOrders(String callOrderId, String putOrderId) { }

    // Custom exceptions
    private static class StraddleExecutionException extends Exception {
        public StraddleExecutionException(String message) {
            super(message);
        }

        public StraddleExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private double callEntryAveragePrice = 0.0;
    private double putEntryAveragePrice = 0.0;

    public StraddleTask(KiteConnect kiteConnect,
                        Long straddleStrategyId,
                        QuoteService quoteService,
                        InstrumentRepository instrumentRepository,
                        StraddleStrategyRepository straddleStrategyRepository,
                        StraddleStrategyExecutionRepository straddleStrategyExecutionRepository,
                        EmailService emailService,
                        TemplateEngine templateEngine) {
        this.kiteConnect = kiteConnect;
        this.straddleStrategyId = straddleStrategyId;
        this.quoteService = quoteService;
        this.instrumentRepository = instrumentRepository;
        this.straddleStrategyRepository = straddleStrategyRepository;
        this.straddleStrategyExecutionRepository = straddleStrategyExecutionRepository;
        this.emailService = emailService;
        this.templateEngine = templateEngine;
    }

    @Override
    public void run() {
        try {
            this.executeStraddleStrategy();
        } catch (StraddleExecutionException e) {
            straddleLogger.error("StraddleExecutionException occurred for strategy id: {} - {}", straddleStrategyId, e.getMessage());
        } catch (KiteException e) {
            straddleLogger.error("KiteException occurred for strategy id: {} - {}", straddleStrategyId, e.getMessage());
        } catch (Exception e) {
            straddleLogger.error("Unexpected exception occurred for strategy id: {} - {}", straddleStrategyId, e.getMessage());
        }
    }

    private void executeStraddleStrategy() throws StraddleExecutionException, KiteException {
        StraddleStrategy straddleStrategy = this.getStraddleStrategy();
        this.validateStrategyActive(straddleStrategy);
        this.logStrategyStart(straddleStrategy);
        Quote underlyingQuote = this.getUnderlyingQuote(straddleStrategy);
        int strike = this.calculateStrike(underlyingQuote, straddleStrategy);
        StraddleInstruments instruments = this.getStraddleInstruments(straddleStrategy, strike);
        StraddleQuotes quotes = this.getStraddleQuotes(instruments);
        if (!quotes.isValid()) {
            throw new StraddleExecutionException("Invalid quotes received for straddle instruments. Stopping execution.");
        }
        this.createStraddleStrategyExecution(straddleStrategy, instruments);
        this.setInitialPrices(quotes);
        StraddleOrders orders = this.placeStraddleOrders(straddleStrategy, instruments);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                this.updateEntryPrices(orders);
            } catch (StraddleExecutionException e) {
                straddleLogger.error("Failed to update entry prices for strategy id: {} - {}", straddleStrategyId, e.getMessage());
            }
        }, ORDER_PRICES_UPDATE_DELAY_SECONDS, TimeUnit.SECONDS);
        if(this.callEntryAveragePrice == 0.0 || this.putEntryAveragePrice == 0.0) {
            straddleLogger.warn("Straddle entry prices not found in the orders. Setting initial prices from quotes.");
            this.setInitialPrices(quotes);
        }
        this.updateStraddleStrategyExecutionEntryPrices(straddleStrategy);
        this.monitorPnl(instruments);
    }

    private void createStraddleStrategyExecution(StraddleStrategy straddleStrategy, StraddleInstruments instruments) {
        String uniqueRunId = this.generateUniqueId(straddleStrategy.getUserId(), straddleStrategy.getInstrument());
        this.execution = new StraddleStrategyExecution();
        this.execution.setExecutionDate(Calendar.getInstance().getTime());
        this.execution.setUserId(straddleStrategy.getUserId());
        this.execution.setStrategyId(straddleStrategy.getId());
        this.execution.setUniqueRunId(uniqueRunId);
        this.execution.setInstrument(straddleStrategy.getInstrument());
        this.execution.setStrikeSelector(straddleStrategy.getUnderlyingStrikeSelector());
        this.execution.setCallStrike(instruments.call.getDisplayName());
        this.execution.setCallQuantity(this.getStrategyQuantity(straddleStrategy, instruments.call));
        this.execution.setPutStrike(instruments.put.getDisplayName());
        this.execution.setPutQuantity(this.getStrategyQuantity(straddleStrategy, instruments.put));
        this.execution.setPaperTrade(straddleStrategy.isPaperTrade());
        this.execution.setCreatedAt(Calendar.getInstance().getTime());
        straddleStrategyExecutionRepository.save(execution);
        straddleLogger.info("[{}] [{}] Straddle strategy execution created with unique run id: {}",
                straddleStrategy.getUserId(), straddleStrategy.getInstrument(), uniqueRunId);
    }

    private void updateStraddleStrategyExecutionEntryPrices(StraddleStrategy straddleStrategy) {
        this.execution.setCallEntryPrice(BigDecimal.valueOf(this.callEntryAveragePrice));
        this.execution.setPutEntryPrice(BigDecimal.valueOf(this.putEntryAveragePrice));
        straddleStrategyExecutionRepository.save(execution);
        straddleLogger.info("[{}] [{}] Straddle strategy execution updated with entry prices for unique run id: {}",
                straddleStrategy.getUserId(), straddleStrategy.getInstrument(), execution.getUniqueRunId());
        this.notifyStraddleEntryExit(false);
    }

    private void updateStraddleStrategyExecutionExitPnl(StraddleStrategy straddleStrategy, double pnl, StraddleQuotes currentQuotes) {
        this.execution.setCallExitPrice(BigDecimal.valueOf(currentQuotes.call.lastPrice));
        this.execution.setPutExitPrice(BigDecimal.valueOf(currentQuotes.put.lastPrice));
        this.execution.setExitPnl(BigDecimal.valueOf(pnl));
        this.execution.setExitedAt(Calendar.getInstance().getTime());
        straddleStrategyExecutionRepository.save(execution);
        straddleLogger.info("[{}] [{}] Straddle strategy execution updated with exit pnl for unique run id: {}",
                straddleStrategy.getUserId(), straddleStrategy.getInstrument(), execution.getUniqueRunId());
        this.notifyStraddleEntryExit(true);
    }

    private StraddleOrders placeStraddleOrders(StraddleStrategy strategy, StraddleInstruments instruments)
            throws StraddleExecutionException {
        try {
            String callOrderId = this.placeOrder(strategy, instruments.call);
            String putOrderId = this.placeOrder(strategy, instruments.put);
            return new StraddleOrders(callOrderId, putOrderId);
        } catch (Exception | KiteException e) {
            throw new StraddleExecutionException("Error placing straddle orders", e);
        }
    }

    private void updateEntryPrices(StraddleOrders orders) throws StraddleExecutionException {
        try {
            if (StringUtils.isNotBlank(orders.callOrderId)) {
                this.callEntryAveragePrice = this.getOrderAveragePrice(orders.callOrderId);
            }
            if (StringUtils.isNotBlank(orders.putOrderId)) {
                this.putEntryAveragePrice = this.getOrderAveragePrice(orders.putOrderId);
            }
        }catch (Exception | KiteException e) {
            throw new StraddleExecutionException("Error updating entry prices", e);
        }
    }

    private Double getOrderAveragePrice(String orderId) throws IOException, KiteException {
        Order macthedOrder = kiteConnect.getOrders().stream()
                .filter(order -> order.orderId.equals(orderId)).findFirst().orElse(null);
        if(null != macthedOrder) {
            straddleLogger.info("Order with id: {} found in the orders with avg price: {}", orderId, macthedOrder.averagePrice);
            return Double.parseDouble(macthedOrder.averagePrice);
        } else {
            straddleLogger.warn("Order with id: {} not found in the orders", orderId);
            return 0.0;
        }
    }

    private int calculateStrike(Quote quote, StraddleStrategy strategy) {
        int strikeStep = strategy.getStrikeStep() != null ? strategy.getStrikeStep() : DEFAULT_STRIKE_STEP;
        int strike = (int) (Math.round(quote.lastPrice / strikeStep) * strikeStep);
        straddleLogger.info("[{}] [{}] Straddle strike is: {} using step: {}", strategy.getUserId(), strategy.getInstrument(), strike, strikeStep);
        return strike;
    }

    private StraddleInstruments getStraddleInstruments(StraddleStrategy strategy, int strike) throws StraddleExecutionException {
        try {
            Instrument callInstrument = this.getInstrumentByStrike(strategy, strike, CALL_OPTION);
            Instrument putInstrument = this.getInstrumentByStrike(strategy, strike, PUT_OPTION);
            return new StraddleInstruments(callInstrument, putInstrument);
        } catch (Exception e) {
            throw new StraddleExecutionException("Error fetching straddle instruments for strike: " + strike, e);
        }
    }

    private StraddleQuotes getStraddleQuotes(StraddleInstruments instruments) {
        String callInstrumentName = this.getQuoteInstrumentName(instruments.call);
        String putInstrumentName = this.getQuoteInstrumentName(instruments.put);
        Map<String, Quote> quotes = this.getLatestQuotes(new String[]{callInstrumentName, putInstrumentName});
        return new StraddleQuotes(quotes.get(callInstrumentName), quotes.get(putInstrumentName));
    }

    private void setInitialPrices(StraddleQuotes quotes) {
        this.callEntryAveragePrice = quotes.call.lastPrice;
        this.putEntryAveragePrice = quotes.put.lastPrice;
    }

    private void validateStrategyActive(StraddleStrategy strategy) throws StraddleExecutionException {
        if (strategy == null) {
            throw new StraddleExecutionException("Strategy not found for id: " + straddleStrategyId);
        }
        if (!strategy.getIsActive()) {
            throw new StraddleExecutionException("Strategy is not active");
        }
        if (strategy.getLots() <= 0) {
            throw new StraddleExecutionException("Invalid lot size: " + strategy.getLots());
        }
    }

    private void logStrategyStart(StraddleStrategy strategy) {
        straddleLogger.info("[{}] [{}] Straddle task started for strategy id: {}",
                strategy.getUserId(), strategy.getInstrument(), strategy.getId());
        straddleLogger.info("[{}] [{}] Entering straddle. Paper trade mode: {}",
                strategy.getUserId(), strategy.getInstrument(), strategy.isPaperTrade());
    }

    private Quote getUnderlyingQuote(StraddleStrategy strategy) throws StraddleExecutionException {
        Quote quote;
        try {
            String exchange = strategy.getExchange();
            String instrument = strategy.getInstrument();
            if (FUTURE_SELECTOR.equals(strategy.getUnderlyingStrikeSelector())) {
                Instrument underlyingFuture = this.getUnderlyingFutureForCurrentMonth(strategy);
                if (underlyingFuture != null) {
                    exchange = underlyingFuture.getExchange();
                    instrument = underlyingFuture.getTradingSymbol();
                }
                straddleLogger.info("[{}] [{}] Using future for underlying: {}", strategy.getUserId(), strategy.getInstrument(), instrument);
            } else {
                straddleLogger.info("[{}] [{}] Using index for underlying: {}", strategy.getUserId(), strategy.getInstrument(), strategy.getIndex());
            }
            quote = this.getLatestQuote(exchange + ":" + instrument);
            if (quote == null) {
                throw new StraddleExecutionException("Unable to fetch quote for: " + instrument);
            }
            straddleLogger.info("[{}] [{}] Fetched quote for {} with price: {}", strategy.getUserId(), strategy.getInstrument(), instrument, quote.lastPrice);
        } catch (Exception e) {
            throw new StraddleExecutionException("Error fetching underlying quote", e);
        }
        return quote;
    }

    private  List<Position> getPositions() throws IOException, KiteException {
        Map<String, List<Position>> positionsMap = kiteConnect.getPositions();
        return positionsMap.get("net");
    }

    private Quote getLatestQuote(String instrument) {
        Map<String, Quote> quote = quoteService.getQuote(instrument);
        return quote.get(instrument);
    }

    private Map<String, Quote> getLatestQuotes(String[] instruments) {
        return quoteService.getQuotes(instruments);
    }

    private Instrument getInstrumentByStrike(StraddleStrategy straddleStrategy, int strike, String optionType) {
        List<Instrument> expiryInstruments;
        if(straddleStrategy.getExpiryScope().equals(CURRENT_EXPIRY)) {
            expiryInstruments = instrumentRepository.findCurrentWeekOptionExpiryInstruments(straddleStrategy.getTradingSegment(), straddleStrategy.getIndex());
        } else {
            expiryInstruments = instrumentRepository.findNextWeekOptionExpiryInstruments(straddleStrategy.getTradingSegment(), straddleStrategy.getIndex());
        }
        return expiryInstruments.stream().filter(i -> i.getStrike().equals(String.valueOf(strike)) &&
                        i.getInstrumentType().equals(optionType)).findFirst()
                .orElseThrow(() -> new RuntimeException(optionType+" instrument not found for strike: " + strike));
    }

    private Instrument getUnderlyingFutureForCurrentMonth(StraddleStrategy straddleStrategy) {
        List<Instrument> expiryInstruments = instrumentRepository.findCurrentWeekOptionExpiryInstruments(straddleStrategy.getUnderlyingSegment(), straddleStrategy.getIndex());
        if(!expiryInstruments.isEmpty()) {
            return expiryInstruments.get(0);
        } else {
            straddleLogger.error("[{}] [{}] No underlying future found for current month for index: {}. It will use the underlying index for strike selection", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getIndex());
            return null;
        }
    }

    private String placeOrder(StraddleStrategy straddleStrategy, Instrument instrument) throws Exception, KiteException {
        String orderId = "";
        straddleLogger.info("[{}] [{}] Placing entry order for instrument: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), instrument.getDisplayName());
        OrderParams orderParams = new OrderParams();
        orderParams.exchange = instrument.getExchange();
        orderParams.tradingsymbol = instrument.getTradingSymbol();
        orderParams.transactionType = straddleStrategy.getSide().equals(SIDE.SHORT.getValue()) ? Constants.TRANSACTION_TYPE_SELL : Constants.TRANSACTION_TYPE_BUY;
        orderParams.quantity = this.getStrategyQuantity(straddleStrategy, instrument);
        orderParams.orderType = Constants.ORDER_TYPE_MARKET;
        // currently only market orders are supported
        orderParams.product = Constants.PRODUCT_MIS;
        orderParams.tag = "straddle-entry-order";
        if(straddleStrategy.isPaperTrade()) {
            straddleLogger.info("[{}] [{}] Paper entry order placed successfully for instrument: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), instrument.getDisplayName());
        } else {
            Order order = kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            orderId = order.orderId;
            straddleLogger.info("[{}] [{}] Live entry order placed successfully for instrument: {} Id: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), instrument.getDisplayName(), orderId);
        }
        return orderId;
    }

    private int sideMultiplier(StraddleStrategy straddleStrategy) {
        return straddleStrategy.getSide().equals(SIDE.LONG.getValue()) ? 1 : -1;
    }

    private int getStrategyQuantity(StraddleStrategy straddleStrategy, Instrument instrument) {
        return straddleStrategy.getLots() * instrument.getLotSize();
    }

    private void monitorPnl(StraddleInstruments instruments) {
        AtomicBoolean isTrailingActive = new AtomicBoolean(false);
        AtomicReference<Double> peakPnl = new AtomicReference<>(Double.NEGATIVE_INFINITY);
        AtomicReference<Double> trailingStopPnl = new AtomicReference<>(Double.NEGATIVE_INFINITY);
        AtomicReference<Double> previousPnl = new AtomicReference<>(0.0);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                StraddleStrategy straddleStrategy = this.getStraddleStrategy();
                StraddleQuotes currentQuotes = this.getValidQuotes(straddleStrategy, instruments);
                if (currentQuotes == null) {
                    return;
                }
                if (this.shouldStopMonitoring(straddleStrategy)) {
                    this.updateStraddleStrategyExecutionExitPnl(straddleStrategy, previousPnl.get(), currentQuotes);
                    executor.shutdown();
                    return;
                }
                if (!this.arePositionsValid(straddleStrategy, instruments)) {
                    return;
                }
                double pnl = this.calculatePnl(straddleStrategy, currentQuotes, instruments.call);
                previousPnl.set(pnl);
                straddleLogger.debug("[{}] [{}] Current PnL: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), pnl);
                boolean shouldExit = this.shouldExitStrategy(straddleStrategy, pnl, isTrailingActive, peakPnl, trailingStopPnl);
                if (shouldExit) {
                    this.exitPositions(straddleStrategy, instruments);
                    this.updateStraddleStrategyExecutionExitPnl(straddleStrategy, pnl, currentQuotes);
                    executor.shutdown();
                } else {
                    logStrategyStatus(straddleStrategy, currentQuotes, pnl);
                }

            } catch (KiteException e) {
                straddleLogger.error("KiteException in monitorPnl for strategy id: {}  {}", straddleStrategyId, e.getMessage());
            } catch (Exception e) {
                straddleLogger.error("Exception in monitorPnl for strategy id: {}  {}", straddleStrategyId, e.getMessage());
            }
        }, MONITOR_PNL_INITIAL_DELAY_SECONDS, MONITOR_PNL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private boolean shouldStopMonitoring(StraddleStrategy straddleStrategy) {
        if (!straddleStrategy.getIsActive()) {
            straddleLogger.info("[{}] [{}] Straddle strategy is not active anymore. Stopping monitorPnl",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument());
            return true;
        }
        straddleLogger.debug("[{}] [{}] Monitoring pnl for straddle strategy id: {}",
                straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getId());
        return false;
    }

    private boolean arePositionsValid(StraddleStrategy straddleStrategy, StraddleInstruments instruments) {
        if (!validatePositions(straddleStrategy, instruments)) {
            straddleLogger.error("[{}] [{}] Corresponding live positions not found. Skipping monitoring cycle",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument());
            return false;
        }
        return true;
    }

    private StraddleQuotes getValidQuotes(StraddleStrategy straddleStrategy, StraddleInstruments instruments) {
        StraddleQuotes currentQuotes = this.getStraddleQuotes(instruments);
        if (!currentQuotes.isValid()) {
            straddleLogger.error("[{}] [{}] Quotes not received. Skipping monitoring cycle",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument());
            return null;
        }
        return currentQuotes;
    }

    private boolean shouldExitStrategy(StraddleStrategy straddleStrategy, double pnl, AtomicBoolean isTrailingActive,
                                       AtomicReference<Double> peakPnl, AtomicReference<Double> trailingStopPnl) {
        return this.checkStopLoss(straddleStrategy, pnl) || this.checkTimeBasedExit(straddleStrategy, pnl) ||
                this.checkTargetAndTrailing(straddleStrategy, pnl, isTrailingActive, peakPnl, trailingStopPnl);
    }

    private boolean checkStopLoss(StraddleStrategy straddleStrategy, double pnl) {
        if (pnl <= -straddleStrategy.getStopLoss()) {
            straddleLogger.info("[{}] [{}] Straddle strategy stop loss {} hit. Exiting positions at pnl: {}",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getStopLoss(), pnl);
            return true;
        }
        return false;
    }

    private boolean checkTimeBasedExit(StraddleStrategy straddleStrategy, double pnl) {
        Time currentTime = Time.valueOf(LocalTime.now());
        if (currentTime.after(straddleStrategy.getExitTime())) {
            straddleLogger.info("[{}] [{}] Time {} passed exit time {}. Exiting positions at pnl: {}",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument(), currentTime, straddleStrategy.getExitTime(), pnl);
            return true;
        }
        return false;
    }

    private boolean checkTargetAndTrailing(StraddleStrategy straddleStrategy, double pnl, AtomicBoolean isTrailingActive,
                                           AtomicReference<Double> peakPnl, AtomicReference<Double> trailingStopPnl) {
        if (straddleStrategy.isTrailingSl()) {
            return checkTrailingTarget(straddleStrategy, pnl, isTrailingActive, peakPnl, trailingStopPnl);
        } else {
            return checkSimpleTarget(straddleStrategy, pnl);
        }
    }

    private boolean checkSimpleTarget(StraddleStrategy straddleStrategy, double pnl) {
        if (pnl >= straddleStrategy.getTarget()) {
            straddleLogger.info("[{}] [{}] Straddle strategy target {} hit. Exiting positions at pnl: {}",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getTarget(), pnl);
            return true;
        }
        return false;
    }

    private boolean checkTrailingTarget(StraddleStrategy straddleStrategy, double pnl, AtomicBoolean isTrailingActive,
                                        AtomicReference<Double> peakPnl, AtomicReference<Double> trailingStopPnl) {
        if (pnl >= straddleStrategy.getTarget() || isTrailingActive.get()) {
            if (!isTrailingActive.get()) {
               this.activateTrailing(straddleStrategy, pnl, isTrailingActive, peakPnl, trailingStopPnl);
            } else {
                this.updateTrailingStop(straddleStrategy, pnl, peakPnl, trailingStopPnl);
                return this.checkTrailingStop(straddleStrategy, pnl, trailingStopPnl);
            }
        }
        return false;
    }

    private void activateTrailing(StraddleStrategy straddleStrategy, double pnl, AtomicBoolean isTrailingActive,
                                  AtomicReference<Double> peakPnl, AtomicReference<Double> trailingStopPnl) {
        isTrailingActive.set(true);
        peakPnl.set(pnl);
        trailingStopPnl.set(straddleStrategy.getTarget() * TRAILING_STOP_RATIO);
        straddleLogger.info("[{}] [{}] Target hit. Entering trailing mode. Trailing stop set to: {}",
                straddleStrategy.getUserId(), straddleStrategy.getInstrument(), trailingStopPnl.get());
    }

    private void updateTrailingStop(StraddleStrategy straddleStrategy, double pnl, AtomicReference<Double> peakPnl, AtomicReference<Double> trailingStopPnl) {
        if (pnl > peakPnl.get()) {
            double previousPeak = peakPnl.get();
            double percentGain = (pnl - previousPeak) / previousPeak;
            if (percentGain >= PEAK_THRESHOLD_PERCENTAGE) {
                peakPnl.set(pnl);
                trailingStopPnl.set(pnl * TRAILING_STOP_RATIO);
                straddleLogger.info("[{}] [{}] PnL rose 10% from previous peak. New peak: {}, New trailing stop: {}",
                        straddleStrategy.getUserId(), straddleStrategy.getInstrument(), pnl, trailingStopPnl.get());
            }
        }
    }

    private boolean checkTrailingStop(StraddleStrategy straddleStrategy, double pnl, AtomicReference<Double> trailingStopPnl) {
        if (pnl <= trailingStopPnl.get()) {
            straddleLogger.info("[{}] [{}] Trailing stop {} hit. Exiting positions at pnl: {}",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument(), trailingStopPnl.get(), pnl);
            return true;
        } else if(pnl >= straddleStrategy.getTarget() * 2) {
            straddleLogger.info("[{}] [{}] Profit has hit 2x of initial target {}. Exiting positions at pnl: {}",
                    straddleStrategy.getUserId(), straddleStrategy.getInstrument(), straddleStrategy.getTarget(), pnl);
            return true;
        }
        return false;
    }

    private void exitPositions(StraddleStrategy straddleStrategy, StraddleInstruments instruments) throws KiteException, Exception {
        this.exitOrder(straddleStrategy, instruments.call);
        this.exitOrder(straddleStrategy, instruments.put);
    }

    private void logStrategyStatus(StraddleStrategy straddleStrategy, StraddleQuotes currentQuotes, double pnl) {
        straddleLogger.debug("[{}] [{}] Strategy active. [Call Entry: {}] [Put Entry: {}] [Call Now: {}] [Put Now: {}]",
                straddleStrategy.getUserId(), straddleStrategy.getInstrument(),
                callEntryAveragePrice, putEntryAveragePrice, currentQuotes.call.lastPrice, currentQuotes.put.lastPrice);
        straddleLogger.info("[{}] [{}] [PNL: {}]", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), pnl);
    }

    private double calculatePnl(StraddleStrategy strategy, StraddleQuotes currentQuotes, Instrument referenceInstrument) {
        double totalEntry = callEntryAveragePrice + putEntryAveragePrice;
        double totalCurrent = currentQuotes.call.lastPrice + currentQuotes.put.lastPrice;
        return -1 * sideMultiplier(strategy) * (totalEntry - totalCurrent) * getStrategyQuantity(strategy, referenceInstrument);
    }

    private boolean validatePositions(StraddleStrategy strategy, StraddleInstruments instruments) {
        if (strategy.isPaperTrade()) {
            return true;
        }
        try {
            List<Position> positions = this.getPositions();
            boolean hasCallPosition = this.hasRequiredPosition(positions, strategy, instruments.call);
            boolean hasPutPosition = this.hasRequiredPosition(positions, strategy, instruments.put);
            return hasCallPosition && hasPutPosition;
        } catch (Exception | KiteException e) {
            straddleLogger.error("Error validating positions: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasRequiredPosition(List<Position> positions, StraddleStrategy strategy, Instrument instrument) {
        int requiredQuantity = this.getStrategyQuantity(strategy, instrument);
        return positions.stream().anyMatch(position ->
                position.tradingSymbol.equals(instrument.getTradingSymbol()) &&
                        (this.sideMultiplier(strategy) * position.netQuantity) >= requiredQuantity);
    }

    private String exitOrder(StraddleStrategy straddleStrategy, Instrument instrument) throws Exception, KiteException {
        String orderId = "";
        straddleLogger.info("[{}] [{}] Placing exit order for instrument: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), instrument.getDisplayName());
        OrderParams orderParams = new OrderParams();
        orderParams.exchange = instrument.getExchange();
        orderParams.tradingsymbol = instrument.getTradingSymbol();
        orderParams.transactionType = straddleStrategy.getSide().equals(SIDE.SHORT.getValue()) ? Constants.TRANSACTION_TYPE_BUY : Constants.TRANSACTION_TYPE_SELL;
        orderParams.quantity = this.getStrategyQuantity(straddleStrategy, instrument);
        orderParams.orderType = Constants.ORDER_TYPE_MARKET;
        orderParams.product = Constants.PRODUCT_MIS;
        orderParams.tag = "straddle-exit-order";
        if(straddleStrategy.isPaperTrade()) {
            straddleLogger.info("[{}] [{}] Paper exit order placed successfully for instrument: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), instrument.getDisplayName());
        } else {
            Order order = kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            orderId = order.orderId;
            straddleLogger.info("[{}] [{}] Live exit order placed successfully for instrument: {} Id: {}", straddleStrategy.getUserId(), straddleStrategy.getInstrument(), instrument.getDisplayName(), orderId);
        }
        return orderId;
    }

    private StraddleStrategy getStraddleStrategy() {
        return straddleStrategyRepository.findById(straddleStrategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Straddle strategy not found for id: " + straddleStrategyId));
    }

    private String getQuoteInstrumentName(Instrument instrument) {
        return instrument.getExchange() + ":" + instrument.getTradingSymbol();
    }

    private String generateUniqueId(String userId, String instrument) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return String.format("%s_%s_%s", userId, instrument, timestamp);
    }

    public void notifyStraddleEntryExit(boolean isExit) {
        StraddleStrategy straddleStrategy = this.getStraddleStrategy();
        StringBuilder subject = new StringBuilder("Algo - Straddle ").append(isExit ? "Exit ":"Entry ").append("Notification-");
        subject.append(straddleStrategy.getUserId()).append("-")
                .append(straddleStrategy.getInstrument()).append("-")
                .append(CommonUtil.formatDateWithSuffix(LocalDate.now()));
        String [] to = new String[] { this.getStraddleStrategy().getCreatedBy() };
        ResendEmailPayload resendEmailPayload = new ResendEmailPayload();
        resendEmailPayload.setTo(to);
        resendEmailPayload.setSubject(subject.toString());
        resendEmailPayload.setHtml(this.getEmailBody(subject.toString(), isExit));
        emailService.sendEmail(resendEmailPayload);
    }

    private String getEmailBody(String subject, boolean isExit) {
        // Create inner content context
        Context context = new Context();
        String formattedCallEntry = NumberFormat.getInstance().format(execution.getCallEntryPrice());
        String formattedPutEntry = NumberFormat.getInstance().format(execution.getPutEntryPrice());
        context.setVariable("formattedCallEntry", formattedCallEntry);
        context.setVariable("formattedPutEntry", formattedPutEntry);
        context.setVariable("execution", this.execution);
        context.setVariable("subject", subject);

        // Process the report template
        String templateName = isExit ? "email/algo-straddle-exit-notification.html" : "email/algo-straddle-entry-notification.html";
        String contentHtml = templateEngine.process(templateName, context);

        // Wrap in base layout
        Context baseContext = new Context();
        baseContext.setVariable("subject", subject);
        baseContext.setVariable("contentHtml", contentHtml);

        return templateEngine.process("email/base-layout", baseContext);

    }
}
