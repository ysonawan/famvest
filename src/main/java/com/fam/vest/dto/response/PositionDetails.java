package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zerodhatech.models.Position;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionDetails {

    private Long sequenceNumber;
    private String type;
    private String userId;
    private String displayName;
    private Long instrumentToken;
    private Double dayPnl;
    private Position position;
}
