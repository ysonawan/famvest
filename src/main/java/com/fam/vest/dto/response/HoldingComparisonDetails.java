package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HoldingComparisonDetails {

    private String userId;
    private String tradingSymbol;
    private String instrument;
    private Long instrumentToken;
    private String type;

    // Historical (snapshot) data
    private Double previousQuantity;
    private Double previousPrice;
    private Double previousValue;

    // Current data
    private Double currentQuantity;
    private Double currentPrice;
    private Double currentValue;

    // Comparison metrics
    private Double minQuantity; // Min of previous and current quantity
    private Double absoluteChange; // Price change in absolute terms
    private Double percentageChange; // Price change percentage
}

