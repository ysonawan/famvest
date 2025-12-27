package com.fam.vest.pojo.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HoldingReportRow {

    private String instrument;
    private Double quantity;
    private Double investedAmount;
    private Double currentValue;
    private Double changeInValueFromLastPeriod;
    private Double changeInValueFromLastPeriodPercentage;
    private Double netProfitToday;
    private Double netProfitTodayPercentage;
}
