package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zerodhatech.models.Profile;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {

    private Long id;
    private String name;
    private String userId;
    private String tokenStatus;
    private String kiteLoginEndPoint;
    private boolean active;
    private Profile profile;
}
