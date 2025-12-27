package com.fam.vest.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Time;
import java.util.Date;

@Data
@Entity
@Table(name = "straddle_strategy", schema = "app_schema")
public class StraddleStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trading_account_user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "side", length = 5, nullable = false)
    private String side;

    @Column(name = "instrument", length = 255, nullable = false)
    private String instrument;

    @Column(name = "exchange", length = 255, nullable = false)
    private String exchange;

    @Column(name = "trading_segment", length = 255, nullable = false)
    private String tradingSegment;

    @Column(name = "underlying_segment", length = 255, nullable = false)
    private String underlyingSegment;

    @Column(name = "index", length = 255, nullable = false)
    private String index;

    @Column(name = "strike_step")
    private Integer strikeStep;

    @Column(name = "underlying_strike_selector", length = 255, nullable = false)
    private String underlyingStrikeSelector;

    @Column(name = "lots", nullable = false)
    private Integer lots;

    @Column(name = "market_order", nullable = false)
    private boolean marketOrder = true;

    @Column(name = "entry_time", nullable = false)
    private Time entryTime;

    @Column(name = "exit_time", nullable = false)
    private Time exitTime;

    @Column(name = "stop_loss")
    private Double stopLoss;

    @Column(name = "target")
    private Double target;

    @Column(name = "paper_trade", nullable = false)
    private boolean isPaperTrade = true;

    @Column(name = "trailing_sl", nullable = false)
    private boolean isTrailingSl = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "expiry_scope", length = 10, nullable = false)
    private String expiryScope = "CURRENT";

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    @Column(name = "last_modified_by", nullable = false)
    private String lastModifiedBy;

    @UpdateTimestamp
    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;
}
