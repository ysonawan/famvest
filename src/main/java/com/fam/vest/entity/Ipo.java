package com.fam.vest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Data
@Entity
@Table(name = "ipo", schema = "app_schema")
public class Ipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "details_url")
    private String detailsUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column(name = "listing_date")
    @Temporal(TemporalType.DATE)
    private Date listingDate;

    @Column(name = "price_range")
    private String priceRange;

    @Column(name = "status")
    private String status;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;
}
