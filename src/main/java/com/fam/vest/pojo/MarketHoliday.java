package com.fam.vest.pojo;

import java.util.List;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MarketHoliday {

    @JsonProperty("open_exchanges")
    private List<ExchangeTiming> openExchanges;

    @JsonProperty("closed_exchanges")
    private List<String> closedExchanges;

    @JsonProperty("holiday_type")
    private String holidayType;

    @JsonProperty("description")
    private String description;

    @JsonProperty("date")
    private String date;
}


