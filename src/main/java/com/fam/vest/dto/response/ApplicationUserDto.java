package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationUserDto {

    private Long id;
    private String fullName;
    private String userName;
    private String role;
    private Date lastLoginTime;
    private Date createdAt;
    private Date updatedAt;
}
