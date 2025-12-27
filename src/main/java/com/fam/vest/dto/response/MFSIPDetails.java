package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zerodhatech.models.MFSIP;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MFSIPDetails {

    private Long sequenceNumber;
    private String userId;
    private MFSIP mfSip;
    private Double lastPrice;
    private Double dayBeforeLastPrice;
    private Double dayChange;
    private Double dayChangePercentage;
}
