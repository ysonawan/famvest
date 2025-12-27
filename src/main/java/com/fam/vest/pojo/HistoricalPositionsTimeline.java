package com.fam.vest.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalPositionsTimeline {

    private Date date;
    private String userId;
    private Long openDerivativePositions;
    private Long exitedDerivativePositions;
    private Double totalEodPnl;
}
