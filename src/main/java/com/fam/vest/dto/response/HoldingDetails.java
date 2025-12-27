package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HoldingDetails {

    private Long sequenceNumber;
    private String type;
    private String userId;
    private String instrument;
    private String tradingSymbol;
    private Long instrumentToken;
    private Double quantity;
    private Double averagePrice;
    private Double lastPrice;
    private Double investedAmount;
    private Double currentValue;
    private Double netPnl;
    private Double netChangePercentage;
    private Double dayPnl;
    private Double dayChange;
    private Double dayChangePercentage;
    private Double dayBeforeLastPrice;

    private String product;
    private String price;
    private int t1Quantity;
    private String collateralQuantity;
    private String collateralType;
    private String isin;
    private Double pnl;
    private String realisedQuantity;
    private String exchange;
    private int usedQuantity;
    private int authorisedQuantity;
    private Date authorisedDate;
    private boolean discrepancy;

}
