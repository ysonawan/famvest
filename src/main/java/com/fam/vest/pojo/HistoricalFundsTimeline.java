package com.fam.vest.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalFundsTimeline {

    private Date date;
    private String userId;
    private Double availableCash;
    private Double availableMargin;
    private Double usedMargin;
    private Double totalCollateral;
}
