package com.fam.vest.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "application_user_trading_account_mapping", schema = "app_schema")
public class ApplicationUserTradingAccountMapping {

    @EmbeddedId
    private ApplicationUserTradingAccountId id = new ApplicationUserTradingAccountId();

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("applicationUserId")
    @JoinColumn(name = "application_user_id", nullable = false)
    private ApplicationUser applicationUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tradingAccountId")
    @JoinColumn(name = "trading_account_id", nullable = false)
    private TradingAccount tradingAccount;
}
