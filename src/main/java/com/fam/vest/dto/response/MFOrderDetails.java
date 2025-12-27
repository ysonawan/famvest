package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zerodhatech.models.MFOrder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MFOrderDetails {

    private Long sequenceNumber;
    private String userId;
    private MFOrder mfOrder;
    private Double lastPrice;
    private Double dayBeforeLastPrice;
    private Double dayChange;
    private Double dayChangePercentage;
}
