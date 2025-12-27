package com.fam.vest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTradingAccountRequestDto {

    private Long id;

    @Size(min = 2, max = 10, message = "Nick name must be between 2 and 10 characters")
    @NotBlank(message = "Nickname is required")
    private String name;

    @NotBlank(message = "Trading user ID is required")
    private String userId;

    private String password; // optional

    private String apiKey;

    private String apiSecret;

    private String totpKey; // optional
}
