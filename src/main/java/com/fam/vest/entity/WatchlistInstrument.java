package com.fam.vest.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "watchlist_instrument", schema = "app_schema")
public class WatchlistInstrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "watchlist_id", nullable = false)
    private Long watchlistId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "trading_symbol", nullable = false)
    private String tradingSymbol;

    @Column(name = "instrument_token")
    private Long instrumentToken;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "segment")
    private String segment;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer sortOrder;

    @Transient
    private Double lastPrice;

    @Transient
    private Double changeAbs;

    @Transient
    private Double change;
}
