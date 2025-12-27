package com.fam.vest.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class IpoDetails {
    private String symbol;
    private String type;
    private String name;
    private String detailsUrl;
    private String logoUrl;
    private Date startDate;
    private Date endDate;
    private Date listingDate;
    private String priceRange;
    private String status;
}
