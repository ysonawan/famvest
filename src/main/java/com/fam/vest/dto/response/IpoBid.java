package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IpoBid {

    private String id;

    @JsonProperty("exchange_id")
    private String exchangeId;

    private String status;
    private int quantity;

    @JsonProperty("auto_cutoff")
    private boolean autoCutoff;

    private int price;
    private int amount;
}
