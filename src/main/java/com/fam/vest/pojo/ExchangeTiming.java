package com.fam.vest.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeTiming {

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("start_time")
    private long startTime;

    @JsonProperty("end_time")
    private long endTime;

}
