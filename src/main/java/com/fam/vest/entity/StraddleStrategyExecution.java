package com.fam.vest.entity;

import lombok.Data;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@Table(name = "straddle_strategy_execution", schema = "app_schema")
public class StraddleStrategyExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date executionDate;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "strategy_id", nullable = false)
    private Long strategyId;

    @Column(name = "unique_run_id", nullable = false, length = 100, unique = true)
    private String uniqueRunId;

    @Column(name = "instrument", nullable = false, length = 255)
    private String instrument;

    @Column(name = "strike_selector", nullable = false, length = 255)
    private String strikeSelector;

    @Column(name = "call_strike", nullable = false, length = 255)
    private String callStrike;

    @Column(name = "call_quantity", nullable = false)
    private Integer callQuantity;

    @Column(name = "call_entry_price", precision = 10, scale = 4)
    private BigDecimal callEntryPrice;

    @Column(name = "call_exit_price", precision = 10, scale = 4)
    private BigDecimal callExitPrice;

    @Column(name = "put_strike", nullable = false, length = 255)
    private String putStrike;

    @Column(name = "put_quantity", nullable = false)
    private Integer putQuantity;

    @Column(name = "put_entry_price", precision = 10, scale = 4)
    private BigDecimal putEntryPrice;

    @Column(name = "put_exit_price", precision = 10, scale = 4)
    private BigDecimal putExitPrice;

    @Column(name = "exit_pnl", nullable = false, precision = 15, scale = 4)
    private BigDecimal exitPnl;

    @Column(name = "paper_trade", nullable = false)
    private Boolean paperTrade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "exited_at", nullable = false)
    private Date exitedAt;

}
