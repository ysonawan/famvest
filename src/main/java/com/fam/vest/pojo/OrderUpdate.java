package com.fam.vest.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderUpdate {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("unfilled_quantity")
    private int unfilledQuantity;

    @JsonProperty("app_id")
    private int appId;

    private String checksum;

    @JsonProperty("placed_by")
    private String placedBy;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("exchange_order_id")
    private String exchangeOrderId;

    @JsonProperty("parent_order_id")
    private String parentOrderId;

    private String status;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("status_message_raw")
    private String statusMessageRaw;

    @JsonProperty("order_timestamp")
    private String orderTimestamp;

    @JsonProperty("exchange_update_timestamp")
    private String exchangeUpdateTimestamp;

    @JsonProperty("exchange_timestamp")
    private String exchangeTimestamp;

    private String variety;
    private String exchange;
    private String tradingsymbol;

    @JsonProperty("instrument_token")
    private long instrumentToken;

    @JsonProperty("order_type")
    private String orderType;

    @JsonProperty("transaction_type")
    private String transactionType;

    private String validity;
    private String product;
    private int quantity;

    @JsonProperty("disclosed_quantity")
    private int disclosedQuantity;

    private double price;

    @JsonProperty("trigger_price")
    private double triggerPrice;

    @JsonProperty("average_price")
    private double averagePrice;

    @JsonProperty("filled_quantity")
    private int filledQuantity;

    @JsonProperty("pending_quantity")
    private int pendingQuantity;

    @JsonProperty("cancelled_quantity")
    private int cancelledQuantity;

    @JsonProperty("market_protection")
    private int marketProtection;

    private Object meta;
    private String tag;
    private String guid;
}
