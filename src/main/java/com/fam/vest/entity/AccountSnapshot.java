package com.fam.vest.entity;

import com.fam.vest.dto.response.*;
import com.fam.vest.entity.converter.*;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "account_snapshot", schema = "app_schema", uniqueConstraints = {
    @UniqueConstraint(columnNames = "snapshot_date")
})
public class AccountSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date snapshotDate;

    @Column(name = "holdings", columnDefinition = "jsonb")
    @Convert(converter = HoldingsJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<HoldingDetails> holdings;

    @Column(name = "orders", columnDefinition = "jsonb")
    @Convert(converter = OrdersJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<OrderDetails> orders;

    @Column(name = "positions", columnDefinition = "jsonb")
    @Convert(converter = PositionsJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<PositionDetails> positions;

    @Column(name = "funds", columnDefinition = "jsonb")
    @Convert(converter = FundsJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<FundDetails> funds;

    @Column(name = "mf_orders", columnDefinition = "jsonb")
    @Convert(converter = MfOrdersJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<MFOrderDetails> mfOrders;

    @Column(name = "mf_sips", columnDefinition = "jsonb")
    @Convert(converter = MfSipsJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<MFSIPDetails> mfSips;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

}
