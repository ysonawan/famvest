package com.fam.vest.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "instrument", schema = "app_schema",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"exchange", "trading_symbol"})})
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument_token")
    private Long instrumentToken;

    @Column(name = "exchange_token")
    private Long exchangeToken;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "trading_symbol", nullable = false)
    private String tradingSymbol;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Column(name = "name")
    private String name;

    @Column(name = "last_price")
    private Double lastPrice;

    @Column(name = "expiry")
    private Date expiry;

    @Column(name = "strike")
    private String strike;

    @Column(name = "tick_size")
    private Double tickSize;

    @Column(name = "lot_size")
    private Integer lotSize;

    @Column(name = "instrument_type")
    private String instrumentType;

    @Column(name = "segment")
    private String segment;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

}
