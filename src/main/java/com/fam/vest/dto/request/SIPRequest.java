package com.fam.vest.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SIPRequest {

    private String frequency;
    private int day;
    private int instalments;
    private double amount;
    private String status;
    private String sipId;
}
