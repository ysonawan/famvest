package com.fam.vest.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zerodhatech.models.MarginCalculationParams;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarginCalculationRequest {

    private String tradingAccountId;
    private MarginCalculationParams marginCalculationParams;

}
