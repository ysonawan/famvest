package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InvestorType {

    private String code;
    private String description;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("min_price")
    private int minPrice;

    @JsonProperty("max_price")
    private int maxPrice;

    @JsonProperty("cutoff_price")
    private int cutoffPrice;

    @JsonProperty("cutoff_disabled")
    private boolean cutoffDisabled;

    @JsonProperty("restrict_downsize")
    private boolean restrictDownsize;

    @JsonProperty("cancel_disabled")
    private boolean cancelDisabled;

    @JsonProperty("discount_type")
    private String discountType;

    @JsonProperty("discount_value")
    private String discountValue;

    @JsonProperty("discount_amount")
    private int discountAmount;

    @JsonProperty("asba_flag")
    private String asbaFlag;

    @JsonProperty("max_modification_limit")
    private long maxModificationLimit;

    @JsonProperty("min_investment_amount")
    private long minInvestmentAmount;

    @JsonProperty("max_investment_amount")
    private long maxInvestmentAmount;
}
