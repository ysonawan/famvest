package com.fam.vest.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MutualFundNav {

    private String code;
    private String name;

    @JsonProperty("short_name")
    private String shortName;

    private String category;

    @JsonProperty("fund_house")
    private String fundHouse;

    @JsonProperty("fund_name")
    private String fundName;

    @JsonProperty("short_code")
    private String shortCode;

    @JsonProperty("detail_info")
    private String detailInfo;

    @JsonProperty("nav")
    private Nav nav;

    @JsonProperty("last_nav")
    private Nav lastNav;


}
