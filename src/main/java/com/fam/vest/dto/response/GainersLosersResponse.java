package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GainersLosersResponse {

    private Date snapshotDate;
    private Date currentDate;
    private String timeframe;
    private List<HoldingDetails> snapshotHoldings;
    private List<HoldingDetails> currentHoldings;
    private List<HoldingComparisonDetails> topGainers;
    private List<HoldingComparisonDetails> topLosers;
}

