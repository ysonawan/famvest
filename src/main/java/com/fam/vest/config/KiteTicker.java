package com.fam.vest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Depth;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.Tick;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.zerodhatech.ticker.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KiteTicker {
    private String wsuri;
    private OnTicks onTickerArrivalListener;
    private OnConnect onConnectedListener;
    private OnDisconnect onDisconnectedListener;
    private OnError onErrorListener;
    private WebSocket ws;
    private OnOrderUpdate orderUpdateListener;
    public final int NseCM = 1;
    public final int NseFO = 2;
    public final int NseCD = 3;
    public final int BseCM = 4;
    public final int BseFO = 5;
    public final int BseCD = 6;
    public final int McxFO = 7;
    public final int McxSX = 8;
    public final int Indices = 9;
    private final String mSubscribe = "subscribe";
    private final String mUnSubscribe = "unsubscribe";
    private final String mSetMode = "mode";
    public static String modeFull = "full";
    public static String modeQuote = "quote";
    public static String modeLTP = "ltp";
    private long lastPongAt = 0L;
    private Set<Long> subscribedTokens = new HashSet();
    private int maxRetries = 10;
    private int count = 0;
    private Timer timer = null;
    private boolean tryReconnection = false;
    private final int pingInterval = 2500;
    private final int pongCheckInterval = 2500;
    private int nextReconnectInterval = 0;
    private int maxRetryInterval = 30000;
    private Map<Long, String> modeMap;
    private Timer canReconnectTimer = null;
    private boolean canReconnect = true;

    public KiteTicker(String accessToken, String apiKey) {
        if (this.wsuri == null) {
            this.createUrl(accessToken, apiKey);
        }

        try {
            this.ws = (new WebSocketFactory()).createSocket(this.wsuri);
        } catch (IOException e) {
            if (this.onErrorListener != null) {
                this.onErrorListener.onError(e);
            }

            return;
        }

        this.ws.addListener(this.getWebsocketAdapter());
        this.modeMap = new HashMap();
    }

    private TimerTask getTask() {
        TimerTask checkForRestartTask = new TimerTask() {
            public void run() {
                if (KiteTicker.this.lastPongAt != 0L) {
                    Date currentDate = new Date();
                    long timeInterval = currentDate.getTime() - KiteTicker.this.lastPongAt;
                    if (timeInterval >= 5000L) {
                        KiteTicker.this.doReconnect();
                    }

                }
            }
        };
        return checkForRestartTask;
    }

    public void doReconnect() {
        if (this.tryReconnection) {
            if (this.nextReconnectInterval == 0) {
                this.nextReconnectInterval = (int)((double)2000.0F * Math.pow((double)2.0F, (double)this.count));
            } else {
                this.nextReconnectInterval = (int)((double)this.nextReconnectInterval * Math.pow((double)2.0F, (double)this.count));
            }

            if (this.nextReconnectInterval > this.maxRetryInterval) {
                this.nextReconnectInterval = this.maxRetryInterval;
            }

            if (this.count <= this.maxRetries) {
                if (this.canReconnect) {
                    ++this.count;
                    this.reconnect(new ArrayList(this.subscribedTokens));
                    this.canReconnect = false;
                    this.canReconnectTimer = new Timer();
                    this.canReconnectTimer.schedule(new TimerTask() {
                        public void run() {
                            KiteTicker.this.canReconnect = true;
                        }
                    }, (long)this.nextReconnectInterval);
                }
            } else if (this.count > this.maxRetries && this.timer != null) {
                this.timer.cancel();
                this.timer = null;
            }

        }
    }

    public void setTryReconnection(boolean retry) {
        this.tryReconnection = retry;
    }

    public void setOnErrorListener(OnError listener) {
        this.onErrorListener = listener;
    }

    public void setMaximumRetries(int maxRetries) throws KiteException {
        if (maxRetries > 0) {
            this.maxRetries = maxRetries;
        } else {
            throw new KiteException("Maximum retries can't be less than 0");
        }
    }

    public void setMaximumRetryInterval(int interval) throws KiteException {
        if (interval >= 5) {
            this.maxRetryInterval = interval * 1000;
        } else {
            throw new KiteException("Maximum retry interval can't be less than 0");
        }
    }

    private void createUrl(String accessToken, String apiKey) {
        //this.wsuri = (new Routes()).getWsuri().replace(":access_token", accessToken).replace(":api_key", apiKey);
        String customWsUri = "wss://ws.zerodha.com/?api_key=:api_key&enctoken=:enc_token&user-agent=kite3-web&version=3.0.0";
        this.wsuri = customWsUri.replace(":api_key", apiKey).replace(":enc_token", accessToken);
    }

    public void setOnTickerArrivalListener(OnTicks onTickerArrivalListener) {
        this.onTickerArrivalListener = onTickerArrivalListener;
    }

    public void setOnConnectedListener(OnConnect listener) {
        this.onConnectedListener = listener;
    }

    public void setOnDisconnectedListener(OnDisconnect listener) {
        this.onDisconnectedListener = listener;
    }

    public void setOnOrderUpdateListener(OnOrderUpdate listener) {
        this.orderUpdateListener = listener;
    }

    public void connect() {
        try {
            this.ws.setPingInterval(2500L);
            this.ws.connect();
        } catch (WebSocketException e) {
            e.printStackTrace();
            if (this.onErrorListener != null) {
                this.onErrorListener.onError(e);
            }

            if (this.tryReconnection && this.timer == null) {
                if (this.lastPongAt == 0L) {
                    this.lastPongAt = 1L;
                }

                this.timer = new Timer();
                this.timer.scheduleAtFixedRate(this.getTask(), 0L, 2500L);
            }
        }

    }

    public WebSocketAdapter getWebsocketAdapter() {
        return new WebSocketAdapter() {
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                KiteTicker.this.count = 0;
                KiteTicker.this.nextReconnectInterval = 0;
                if (KiteTicker.this.onConnectedListener != null) {
                    KiteTicker.this.onConnectedListener.onConnected();
                }

                if (KiteTicker.this.tryReconnection) {
                    if (KiteTicker.this.timer != null) {
                        KiteTicker.this.timer.cancel();
                    }

                    KiteTicker.this.timer = new Timer();
                    KiteTicker.this.timer.scheduleAtFixedRate(KiteTicker.this.getTask(), 0L, 2500L);
                }

            }

            public void onTextMessage(WebSocket websocket, String message) {
                KiteTicker.this.parseTextMessage(message);
            }

            public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                try {
                    super.onBinaryMessage(websocket, binary);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (KiteTicker.this.onErrorListener != null) {
                        KiteTicker.this.onErrorListener.onError(e);
                    }
                }

                ArrayList<Tick> tickerData = KiteTicker.this.parseBinary(binary);
                if (KiteTicker.this.onTickerArrivalListener != null) {
                    KiteTicker.this.onTickerArrivalListener.onTicks(tickerData);
                }

            }

            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) {
                try {
                    super.onPongFrame(websocket, frame);
                    Date date = new Date();
                    KiteTicker.this.lastPongAt = date.getTime();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (KiteTicker.this.onErrorListener != null) {
                        KiteTicker.this.onErrorListener.onError(e);
                    }
                }

            }

            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                if (KiteTicker.this.onDisconnectedListener != null) {
                    KiteTicker.this.onDisconnectedListener.onDisconnected();
                }

            }

            public void onError(WebSocket websocket, WebSocketException cause) {
                try {
                    super.onError(websocket, cause);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (KiteTicker.this.onErrorListener != null) {
                        KiteTicker.this.onErrorListener.onError(e);
                    }
                }

            }
        };
    }

    public void disconnect() {
        if (this.timer != null) {
            this.timer.cancel();
        }

        if (this.ws != null && this.ws.isOpen()) {
            this.ws.disconnect();
            this.subscribedTokens = new HashSet();
            this.modeMap.clear();
        }

    }

    private void nonUserDisconnect() {
        if (this.ws != null) {
            this.ws.disconnect();
        }

    }

    public boolean isConnectionOpen() {
        return this.ws != null && this.ws.isOpen();
    }

    public void setMode(ArrayList<Long> tokens, String mode) {
        JSONObject jobj = new JSONObject();

        try {
            JSONArray list = new JSONArray();
            JSONArray listMain = new JSONArray();
            listMain.put(0, mode);

            for(int i = 0; i < tokens.size(); ++i) {
                list.put(i, tokens.get(i));
            }

            listMain.put(1, list);
            jobj.put("a", "mode");
            jobj.put("v", listMain);

            for(int i = 0; i < tokens.size(); ++i) {
                this.modeMap.put(tokens.get(i), mode);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (this.ws != null) {
            this.ws.sendText(jobj.toString());
        }

    }

    public void subscribe(ArrayList<Long> tokens) {
        if (this.ws != null) {
            if (this.ws.isOpen()) {
                this.createTickerJsonObject(tokens, "subscribe");
                this.ws.sendText(this.createTickerJsonObject(tokens, "subscribe").toString());
                this.subscribedTokens.addAll(tokens);

                for(int i = 0; i < tokens.size(); ++i) {
                    this.modeMap.put(tokens.get(i), modeQuote);
                }
            } else if (this.onErrorListener != null) {
                this.onErrorListener.onError(new KiteException("ticker is not connected", 504));
            }
        } else if (this.onErrorListener != null) {
            this.onErrorListener.onError(new KiteException("ticker is null not connected", 504));
        }

    }

    private JSONObject createTickerJsonObject(ArrayList<Long> tokens, String action) {
        JSONObject jobj = new JSONObject();

        try {
            JSONArray list = new JSONArray();

            for(int i = 0; i < tokens.size(); ++i) {
                list.put(i, tokens.get(i));
            }

            jobj.put("v", list);
            jobj.put("a", action);
        } catch (JSONException var6) {
        }

        return jobj;
    }

    public void unsubscribe(ArrayList<Long> tokens) {
        if (this.ws != null && this.ws.isOpen()) {
            this.ws.sendText(this.createTickerJsonObject(tokens, "unsubscribe").toString());
            this.subscribedTokens.removeAll(tokens);

            for(int i = 0; i < tokens.size(); ++i) {
                this.modeMap.remove(tokens.get(i));
            }
        }

    }

    private ArrayList<Tick> parseBinary(byte[] binaryPackets) {
        ArrayList<Tick> ticks = new ArrayList();
        ArrayList<byte[]> packets = this.splitPackets(binaryPackets);

        for(int i = 0; i < packets.size(); ++i) {
            byte[] bin = (byte[])packets.get(i);
            byte[] t = Arrays.copyOfRange(bin, 0, 4);
            int x = ByteBuffer.wrap(t).getInt();
            int segment = x & 255;
            int dec1 = segment == 3 ? 10000000 : (segment == 6 ? 10000 : 100);
            if (bin.length == 8) {
                Tick tick = this.getLtpQuote(bin, x, dec1, segment != 9);
                ticks.add(tick);
            } else if (bin.length != 28 && bin.length != 32) {
                if (bin.length == 44) {
                    Tick tick = this.getQuoteData(bin, x, dec1, segment != 9);
                    ticks.add(tick);
                } else if (bin.length == 184) {
                    Tick tick = this.getQuoteData(bin, x, dec1, segment != 9);
                    tick.setMode(modeFull);
                    ticks.add(this.getFullData(bin, dec1, tick));
                }
            } else {
                Tick tick = this.getIndeciesData(bin, x, segment != 9);
                ticks.add(tick);
            }
        }

        return ticks;
    }

    private Tick getIndeciesData(byte[] bin, int x, boolean tradable) {
        int dec = 100;
        Tick tick = new Tick();
        tick.setMode(modeQuote);
        tick.setTradable(tradable);
        tick.setInstrumentToken((long)x);
        double lastTradedPrice = this.convertToDouble(this.getBytes(bin, 4, 8)) / (double)dec;
        tick.setLastTradedPrice(lastTradedPrice);
        tick.setHighPrice(this.convertToDouble(this.getBytes(bin, 8, 12)) / (double)dec);
        tick.setLowPrice(this.convertToDouble(this.getBytes(bin, 12, 16)) / (double)dec);
        tick.setOpenPrice(this.convertToDouble(this.getBytes(bin, 16, 20)) / (double)dec);
        double closePrice = this.convertToDouble(this.getBytes(bin, 20, 24)) / (double)dec;
        tick.setClosePrice(closePrice);
        this.setChangeForTick(tick, lastTradedPrice, closePrice);
        if (bin.length > 28) {
            tick.setMode(modeFull);
            long tickTimeStamp = this.convertToLong(this.getBytes(bin, 28, 32)) * 1000L;
            if (this.isValidDate(tickTimeStamp)) {
                tick.setTickTimestamp(new Date(tickTimeStamp));
            } else {
                tick.setTickTimestamp((Date)null);
            }
        }

        return tick;
    }

    private Tick getLtpQuote(byte[] bin, int x, int dec1, boolean tradable) {
        Tick tick1 = new Tick();
        tick1.setMode(modeLTP);
        tick1.setTradable(tradable);
        tick1.setInstrumentToken((long)x);
        tick1.setLastTradedPrice(this.convertToDouble(this.getBytes(bin, 4, 8)) / (double)dec1);
        return tick1;
    }

    private Tick getQuoteData(byte[] bin, int x, int dec1, boolean tradable) {
        Tick tick2 = new Tick();
        tick2.setMode(modeQuote);
        tick2.setInstrumentToken((long)x);
        tick2.setTradable(tradable);
        double lastTradedPrice = this.convertToDouble(this.getBytes(bin, 4, 8)) / (double)dec1;
        tick2.setLastTradedPrice(lastTradedPrice);
        tick2.setLastTradedQuantity(this.convertToDouble(this.getBytes(bin, 8, 12)));
        tick2.setAverageTradePrice(this.convertToDouble(this.getBytes(bin, 12, 16)) / (double)dec1);
        tick2.setVolumeTradedToday(this.convertToLong(this.getBytes(bin, 16, 20)));
        tick2.setTotalBuyQuantity(this.convertToDouble(this.getBytes(bin, 20, 24)));
        tick2.setTotalSellQuantity(this.convertToDouble(this.getBytes(bin, 24, 28)));
        tick2.setOpenPrice(this.convertToDouble(this.getBytes(bin, 28, 32)) / (double)dec1);
        tick2.setHighPrice(this.convertToDouble(this.getBytes(bin, 32, 36)) / (double)dec1);
        tick2.setLowPrice(this.convertToDouble(this.getBytes(bin, 36, 40)) / (double)dec1);
        double closePrice = this.convertToDouble(this.getBytes(bin, 40, 44)) / (double)dec1;
        tick2.setClosePrice(closePrice);
        this.setChangeForTick(tick2, lastTradedPrice, closePrice);
        return tick2;
    }

    private void setChangeForTick(Tick tick, double lastTradedPrice, double closePrice) {
        if (closePrice != (double)0.0F) {
            tick.setNetPriceChangeFromClosingPrice((lastTradedPrice - closePrice) * (double)100.0F / closePrice);
        } else {
            tick.setNetPriceChangeFromClosingPrice((double)0.0F);
        }

    }

    private Tick getFullData(byte[] bin, int dec, Tick tick) {
        long lastTradedtime = this.convertToLong(this.getBytes(bin, 44, 48)) * 1000L;
        if (this.isValidDate(lastTradedtime)) {
            tick.setLastTradedTime(new Date(lastTradedtime));
        } else {
            tick.setLastTradedTime((Date)null);
        }

        tick.setOi(this.convertToDouble(this.getBytes(bin, 48, 52)));
        tick.setOpenInterestDayHigh(this.convertToDouble(this.getBytes(bin, 52, 56)));
        tick.setOpenInterestDayLow(this.convertToDouble(this.getBytes(bin, 56, 60)));
        long tickTimeStamp = this.convertToLong(this.getBytes(bin, 60, 64)) * 1000L;
        if (this.isValidDate(tickTimeStamp)) {
            tick.setTickTimestamp(new Date(tickTimeStamp));
        } else {
            tick.setTickTimestamp((Date)null);
        }

        tick.setMarketDepth(this.getDepthData(bin, dec, 64, 184));
        return tick;
    }

    private Map<String, ArrayList<Depth>> getDepthData(byte[] bin, int dec, int start, int end) {
        byte[] depthBytes = this.getBytes(bin, start, end);
        int s = 0;
        ArrayList<Depth> buy = new ArrayList();
        ArrayList<Depth> sell = new ArrayList();

        for(int k = 0; k < 10; ++k) {
            s = k * 12;
            Depth depth = new Depth();
            depth.setQuantity((int)this.convertToDouble(this.getBytes(depthBytes, s, s + 4)));
            depth.setPrice(this.convertToDouble(this.getBytes(depthBytes, s + 4, s + 8)) / (double)dec);
            depth.setOrders((int)this.convertToDouble(this.getBytes(depthBytes, s + 8, s + 10)));
            if (k < 5) {
                buy.add(depth);
            } else {
                sell.add(depth);
            }
        }

        Map<String, ArrayList<Depth>> depthMap = new HashMap();
        depthMap.put("buy", buy);
        depthMap.put("sell", sell);
        return depthMap;
    }

    private ArrayList<byte[]> splitPackets(byte[] bin) {
        ArrayList<byte[]> packets = new ArrayList();
        int noOfPackets = this.getLengthFromByteArray(this.getBytes(bin, 0, 2));
        int j = 2;

        for(int i = 0; i < noOfPackets; ++i) {
            int sizeOfPacket = this.getLengthFromByteArray(this.getBytes(bin, j, j + 2));
            byte[] packet = Arrays.copyOfRange(bin, j + 2, j + 2 + sizeOfPacket);
            packets.add(packet);
            j = j + 2 + sizeOfPacket;
        }

        return packets;
    }

    private byte[] getBytes(byte[] bin, int start, int end) {
        return Arrays.copyOfRange(bin, start, end);
    }

    private double convertToDouble(byte[] bin) {
        ByteBuffer bb = ByteBuffer.wrap(bin);
        bb.order(ByteOrder.BIG_ENDIAN);
        if (bin.length < 4) {
            return (double)bb.getShort();
        } else {
            return bin.length < 8 ? (double)bb.getInt() : bb.getDouble();
        }
    }

    private long convertToLong(byte[] bin) {
        ByteBuffer bb = ByteBuffer.wrap(bin);
        bb.order(ByteOrder.BIG_ENDIAN);
        return (long)bb.getInt();
    }

    private int getLengthFromByteArray(byte[] bin) {
        ByteBuffer bb = ByteBuffer.wrap(bin);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort();
    }

    private void reconnect(ArrayList<Long> tokens) {
        this.nonUserDisconnect();

        try {
            this.ws = (new WebSocketFactory()).createSocket(this.wsuri);
        } catch (IOException e) {
            if (this.onErrorListener != null) {
                this.onErrorListener.onError(e);
            }

            return;
        }

        this.ws.addListener(this.getWebsocketAdapter());
        this.connect();
        final OnConnect onUsersConnectedListener = this.onConnectedListener;
        this.setOnConnectedListener(new OnConnect() {
            public void onConnected() {
                if (KiteTicker.this.subscribedTokens.size() > 0) {
                    Map<Long, String> backupModeMap = new HashMap();
                    backupModeMap.putAll(KiteTicker.this.modeMap);
                    ArrayList<Long> tokens = new ArrayList();
                    tokens.addAll(KiteTicker.this.subscribedTokens);
                    KiteTicker.this.subscribe(tokens);
                    Map<String, ArrayList<Long>> modes = new HashMap();

                    for(Map.Entry<Long, String> item : backupModeMap.entrySet()) {
                        if (!modes.containsKey(item.getValue())) {
                            modes.put(item.getValue(), new ArrayList());
                        }

                        ((ArrayList)modes.get(item.getValue())).add(item.getKey());
                    }

                    for(Map.Entry<String, ArrayList<Long>> modeArrayItem : modes.entrySet()) {
                        KiteTicker.this.setMode((ArrayList)modeArrayItem.getValue(), (String)modeArrayItem.getKey());
                    }
                }

                KiteTicker.this.lastPongAt = 0L;
                KiteTicker.this.count = 0;
                KiteTicker.this.nextReconnectInterval = 0;
                KiteTicker.this.onConnectedListener = onUsersConnectedListener;
            }
        });
    }

    private boolean isValidDate(long date) {
        if (date <= 0L) {
            return false;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setLenient(false);
            calendar.setTimeInMillis(date);

            try {
                calendar.getTime();
                return true;
            } catch (Exception var5) {
                return false;
            }
        }
    }

    private void parseTextMessage(String message) {
        try {
            JSONObject data = new JSONObject(message);
            if (!data.has("type")) {
                return;
            }

            String type = data.getString("type");
            if (type.equals("order") && this.orderUpdateListener != null) {
                this.orderUpdateListener.onOrderUpdate(this.getOrder(data));
            }

            if (type.equals("error") && this.onErrorListener != null) {
                this.onErrorListener.onError(data.getString("data"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public Order getOrder(JSONObject data) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return format.parse(jsonElement.getAsString());
                } catch (ParseException var5) {
                    return null;
                }
            }
        });
        Gson gson = gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        return (Order)gson.fromJson(String.valueOf(data.get("data")), Order.class);
    }
}
