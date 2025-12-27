package com.fam.vest.pojo;

import lombok.Data;

@Data
public class HoldingComparisonReport {

    private String userId;
    private String instrument;
    private Double quantity;
    private Double investedAmount;
    private Double currentValue;
    private Double changeInValueFromLastWeek;
    private Double changeInValueFromLastWeekPercentage;
    private Double netProfitToday;
    private Double netProfitTodayPercentage;
    private String status; // "ONGOING", "CLOSED", "NEW"
}

