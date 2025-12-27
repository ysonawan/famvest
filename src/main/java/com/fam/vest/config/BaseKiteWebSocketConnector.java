package com.fam.vest.config;

import com.neovisionaries.ws.client.WebSocketException;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.repository.TradingAccountRepository;
import com.fam.vest.service.TokenService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.*;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public abstract class BaseKiteWebSocketConnector {

    private final KiteConnector kiteConnector;
    protected final TradingAccountRepository tradingAccountRepository;
    protected final TokenService tokenService;
    protected KiteTicker customKiteTicker;
    protected com.zerodhatech.ticker.KiteTicker zerodhaKiteTicker;

    @Value("${fam.vest.app.kite.websocket.subscription.batch.size}")
    protected int websocketBatchSize;

    @Value("${fam.vest.app.data.streaming.user}")
    protected String dataStreamingUser;

    @Value("${fam.vest.app.is.custom.data.streaming:true}")
    protected boolean isCustomDataStreaming;

    public BaseKiteWebSocketConnector(KiteConnector kiteConnector,
                                      TradingAccountRepository tradingAccountRepository,
                                      TokenService tokenService) {
        this.kiteConnector = kiteConnector;
        this.tradingAccountRepository = tradingAccountRepository;
        this.tokenService = tokenService;
    }

    protected void subscribeKiteWebsocket() {
        try {
            log.info("Data streaming user {} in the configuration", dataStreamingUser);
            if (StringUtils.isBlank(dataStreamingUser)) {
                log.error("Data streaming user not found in the configuration. Kite web socket connection skipped.");
                return;
            }
            TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(dataStreamingUser);
            if(null != tradingAccount) {
                log.info("is custom data streaming enabled: {}", isCustomDataStreaming);
                if(!isCustomDataStreaming) {
                    this.zerodhaConnectWebsocket(tradingAccount);
                } else {
                    log.info("Custom data streaming is enabled, using custom kite ticker for user: {}", tradingAccount.getUserId());
                    this.customConnectWebsocket(tradingAccount);                    }
            } else {
                log.error("Trading account for {} does not exist", dataStreamingUser);
            }
        } catch (WebSocketException e) {
            log.error("WebSocketException in {}: {}", this.getClass().getSimpleName(), e.getMessage());
        } catch (IOException e) {
            log.error("IOException in {}: {}", this.getClass().getSimpleName(), e.getMessage());
        } catch (KiteException e) {
            log.error("KiteException in {}: {}", this.getClass().getSimpleName(), e.getMessage());
        }
    }

    protected void zerodhaConnectWebsocket(TradingAccount tradingAccount) throws IOException, WebSocketException, KiteException {
        log.info("Initializing WebSocket connection using zerodha kite ticker with user: {}", tradingAccount.getUserId());
        KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
        if(null != kiteConnect) {
            log.info("KiteConnect instance is available for user: {}, attempting to get quote", tradingAccount.getUserId());
            Map<String, Quote> quote = kiteConnect.getQuote(new String[]{"NSE:NIFTY 50"}); // Test connection
            log.info("Creating KiteTicker using access token", tradingAccount.getUserId());
            zerodhaKiteTicker = new com.zerodhatech.ticker.KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());

            zerodhaKiteTicker.setOnConnectedListener(new OnConnect() {
                @Override
                public void onConnected() {
                    log.info("Kite web socket connection is established");
                }
            });

            zerodhaKiteTicker.setOnDisconnectedListener(new OnDisconnect() {
                @Override
                public void onDisconnected() {
                    log.info("Kite web socket connection is disconnected");
                }
            });
            zerodhaKiteTicker.setOnOrderUpdateListener(new OnOrderUpdate() {
                @Override
                public void onOrderUpdate(Order order) {
                    log.info("Kite web socket order update: {}", order);
                }
            });
            zerodhaKiteTicker.setOnErrorListener(new OnError() {
                @Override
                public void onError(Exception exception) {
                    log.error("Kite web socket exception on listener", exception);
                }

                @Override
                public void onError(KiteException kiteException) {
                    log.error("Kite web socket kiteException on listener", kiteException);
                }

                @Override
                public void onError(String error) {
                    log.error("Kite web socket error on listener", error);
                }
            });
            zerodhaKiteTicker.setOnTickerArrivalListener(new OnTicks() {
                @Override
                public void onTicks(ArrayList<Tick> ticks) {
                    log.debug("Kite web socket on ticker arrival listener. Ticks size: {}", ticks.size());
                    if(ticks.size() > 0) {
                        handleTicks(ticks);
                    }
                }
            });
            zerodhaKiteTicker.setTryReconnection(true);
            zerodhaKiteTicker.setMaximumRetries(10);
            zerodhaKiteTicker.setMaximumRetryInterval(30);

            log.info("Connecting to websocket");
            zerodhaKiteTicker.connect();

            boolean isConnected = zerodhaKiteTicker.isConnectionOpen();
            log.info("Websocket connection status: {}", isConnected);
        } else {
            log.error("KiteConnect instance is null. WebSocket connection will not be established.");
        }
    }

    protected void customConnectWebsocket(TradingAccount tradingAccount) throws IOException, WebSocketException, KiteException {
        log.info("Initializing WebSocket connection using custom kite ticker with user: {}", tradingAccount.getUserId());
        String encToken = tokenService.getENCToken(tradingAccount);
        if(StringUtils.isNotBlank(encToken)) {
            log.info("Enc token received for user: {}", tradingAccount.getUserId());
            encToken = URLEncoder.encode(encToken, "UTF-8");
            log.info("Creating KiteTicker instance with enc token of user: {}", tradingAccount.getUserId());
            customKiteTicker = new KiteTicker(encToken, "kitefront");

            customKiteTicker.setOnConnectedListener(new OnConnect() {
                @Override
                public void onConnected() {
                    log.info("Kite web socket connection is established");
                }
            });

            customKiteTicker.setOnDisconnectedListener(new OnDisconnect() {
                @Override
                public void onDisconnected() {
                    log.info("Kite web socket connection is disconnected");
                }
            });
            customKiteTicker.setOnOrderUpdateListener(new OnOrderUpdate() {
                @Override
                public void onOrderUpdate(Order order) {
                    log.info("Kite web socket order update: {}", order);
                }
            });
            customKiteTicker.setOnErrorListener(new OnError() {
                @Override
                public void onError(Exception exception) {
                    log.error("Kite web socket exception on listener", exception);
                }

                @Override
                public void onError(KiteException kiteException) {
                    log.error("Kite web socket kiteException on listener", kiteException);
                }

                @Override
                public void onError(String error) {
                    log.error("Kite web socket error on listener", error);
                }
            });
            customKiteTicker.setOnTickerArrivalListener(new OnTicks() {
                @Override
                public void onTicks(ArrayList<Tick> ticks) {
                    log.debug("Kite web socket on ticker arrival listener. Ticks size: {}", ticks.size());
                    if(ticks.size() > 0) {
                        handleTicks(ticks);
                    }
                }
            });
            customKiteTicker.setTryReconnection(true);
            customKiteTicker.setMaximumRetries(10);
            customKiteTicker.setMaximumRetryInterval(30);

            log.info("Connecting to websocket");
            customKiteTicker.connect();

            boolean isConnected = customKiteTicker.isConnectionOpen();
            log.info("Websocket connection status: {}", isConnected);
        } else {
            log.error("No enc token received. WebSocket connection will not be established.");
        }
    }

    public void subscribeWebsocket(Set<Long> tokens) {
        for (int index = 0; index < tokens.size(); index += websocketBatchSize) {
            List<Long> tokenList = new ArrayList<>(tokens);
            List<Long> batch = tokenList.subList(index, Math.min(index + websocketBatchSize, tokens.size()));
            ArrayList<Long> batchTokens = new ArrayList<>(batch);
            if(null != zerodhaKiteTicker) {
                zerodhaKiteTicker.subscribe(batchTokens);
                zerodhaKiteTicker.setMode(batchTokens, KiteTicker.modeFull);
            } else if(null != customKiteTicker) {
                customKiteTicker.subscribe(batchTokens);
                customKiteTicker.setMode(batchTokens, KiteTicker.modeFull);
            } else {
                log.error("No KiteTicker instance available for subscribing to tokens: {}", batchTokens);
            }
        }
    }

    public void unsubscribeWebsocket(Set<Long> tokens) {
        if(!tokens.isEmpty()) {
            if(null != zerodhaKiteTicker) {
                zerodhaKiteTicker.unsubscribe(new ArrayList<>(tokens));
            } else if(null != customKiteTicker) {
                customKiteTicker.unsubscribe(new ArrayList<>(tokens));
            } else {
                log.error("No KiteTicker instance available for unsubscribing to tokens: {}", tokens);
            }
        } else {
            log.warn("No tokens provided for unsubscription from kite web socket.");
        }
    }

    @PreDestroy
    public void disconnectWebsocket() {
        log.info("Cleaning up resources and disconnecting WebSocket...");
        if(null != zerodhaKiteTicker) {
            zerodhaKiteTicker.disconnect();
        } else if(null != customKiteTicker) {
            customKiteTicker.disconnect();
        } else {
            log.error("No KiteTicker instance available for disconnection.");
        }
    }

    protected abstract void handleTicks(ArrayList<Tick> ticks);
}
