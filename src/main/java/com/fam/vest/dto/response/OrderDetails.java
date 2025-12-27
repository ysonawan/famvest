package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.zerodhatech.models.Order;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDetails {

    private Long tradingAccountId;
    private Long sequenceNumber;
    private String userId;
    private String displayName;
    private Long instrumentToken;
    private Double lastPrice;
    private Double change;
    private Order order;
}
