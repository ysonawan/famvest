package com.fam.vest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUserTradingAccountId implements Serializable {

    @Column(name = "application_user_id")
    private Long applicationUserId;

    @Column(name = "trading_account_id")
    private Long tradingAccountId;
}
