package com.fam.vest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TradingAccountRequestDto {

    @Size(min = 2, max = 10, message = "Nick name must be between 2 and 10 characters")
    @NotBlank(message = "Nickname is required")
    private String name;

    @NotBlank(message = "Trading user ID is required")
    private String userId;

    private String password; // optional

    @NotBlank(message = "API key is required")
    private String apiKey;

    @NotBlank(message = "API secret is required")
    private String apiSecret;

    private String totpKey; // optional
}
