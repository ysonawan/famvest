package com.fam.vest.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalCandleDataRequest {

    @NotNull(message = "From date is required")
    private Date from;

    @NotNull(message = "To date is required")
    private Date to;

    @NotBlank(message = "Instrument is required")
    private String instrument;

    @NotBlank(message = "Instrument token is required")
    private String instrumentToken;

    @NotBlank(message = "Interval is required")
    private String interval;

    @NotNull(message = "Continuous flag is required")
    private Boolean continuous;

    @NotNull(message = "OI flag is required")
    private Boolean oi;
}
