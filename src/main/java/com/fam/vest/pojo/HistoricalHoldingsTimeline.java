package com.fam.vest.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalHoldingsTimeline {

    private Date date;
    private String userId;
    private Double investedAmount;
    private Double currentValue;
    private Double dayPnl;
    private Double dayChangePercentage;
    private Double netPnl;
    private Double netChangePercentage;

}
