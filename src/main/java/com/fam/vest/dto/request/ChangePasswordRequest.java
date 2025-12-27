package com.fam.vest.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangePasswordRequest {

    private String oldPassword;

    @NotBlank(message = "New Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String newPassword;

    @NotBlank(message = "Confirm Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String confirmPassword;
}

