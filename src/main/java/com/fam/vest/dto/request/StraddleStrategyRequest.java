package com.fam.vest.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.sql.Time;

@Data
public class StraddleStrategyRequest {

    @NotBlank(message = "Trading Account is required")
    private String userId;

    @NotBlank(message = "Side is required")
    @Pattern(regexp = "SHORT|LONG", message = "Side must be either SHORT or LONG")
    private String side;

    @NotBlank(message = "Instrument is required")
    private String instrument;

    @NotBlank(message = "Exchange is required")
    private String exchange;

    @NotBlank(message = "Trading segment is required")
    private String tradingSegment;

    @NotBlank(message = "Underlying segment is required")
    private String underlyingSegment;

    @NotBlank(message = "Index is required")
    private String index;

    @NotNull(message = "Strike step is required")
    @Min(value = 1, message = "Strike step must be greater than 0")
    private Integer strikeStep;

    @NotNull(message = "Lots is required")
    @Min(value = 1, message = "Lots must be at least 1")
    private Integer lots;

    @NotBlank(message = "Order type is required")
    @Pattern(regexp = "MARKET|LIMIT", message = "Order type must be either MARKET or LIMIT")
    private String orderType;

    @NotBlank(message = "Trailing SL is required")
    @Pattern(regexp = "Yes|No", message = "Trailing SL must be either Yes or No")
    private String trailingSl;

    @NotNull(message = "Entry time is required")
    private Time entryTime;

    @NotNull(message = "Exit time is required")
    private Time exitTime;

    @NotBlank(message = "Underlying strike selector is required")
    @Pattern(regexp = "INDEX|FUTURE", message = "Underlying strike selector must be either INDEX or FUTURE")
    private String underlyingStrikeSelector;

    @NotBlank(message = "Trade type is required")
    @Pattern(regexp = "PAPER|LIVE", message = "Trade type must be either PAPER or LIVE")
    private String tradeType;

    @NotNull(message = "Stop loss is required")
    @PositiveOrZero(message = "Stop loss cannot be negative")
    private Double stopLoss;

    @NotNull(message = "Target is required")
    @PositiveOrZero(message = "Target cannot be negative")
    private Double target;

    @NotBlank(message = "Expiry scope is required")
    @Pattern(regexp = "CURRENT|NEXT", message = "Expiry scope must be CURRENT or NEXT")
    private String expiryScope;
}
