package com.fam.vest.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zerodhatech.models.MarginCalculationParams;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CombinedMarginCalculationRequest {

    private String tradingAccountId;
    private boolean includeExistingPositions;
    private List<MarginCalculationParams> marginCalculationParams;

}
