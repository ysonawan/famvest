package com.fam.vest.entity;

import com.fam.vest.entity.converter.HistoricalFundsTimelineJsonNodeConverter;
import com.fam.vest.entity.converter.HistoricalHoldingsTimelineJsonNodeConverter;
import com.fam.vest.entity.converter.HistoricalMfSipsTimelineJsonNodeConverter;
import com.fam.vest.entity.converter.HistoricalPositionsTimelineJsonNodeConverter;
import com.fam.vest.pojo.HistoricalFundsTimeline;
import com.fam.vest.pojo.HistoricalHoldingsTimeline;
import com.fam.vest.pojo.HistoricalMfSipsTimeline;
import com.fam.vest.pojo.HistoricalPositionsTimeline;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "historical_timeline_values", schema = "app_schema", uniqueConstraints = {
    @UniqueConstraint(columnNames = "date")
})
public class HistoricalTimelineValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date date;

    @Column(name = "holdings", columnDefinition = "jsonb")
    @Convert(converter = HistoricalHoldingsTimelineJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<HistoricalHoldingsTimeline> historicalHoldingsTimelines;

    @Column(name = "mf_sips", columnDefinition = "jsonb")
    @Convert(converter = HistoricalMfSipsTimelineJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<HistoricalMfSipsTimeline> historicalMfSipsTimelines;

    @Column(name = "funds", columnDefinition = "jsonb")
    @Convert(converter = HistoricalFundsTimelineJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<HistoricalFundsTimeline> historicalFundsTimelines;

    @Column(name = "positions", columnDefinition = "jsonb")
    @Convert(converter = HistoricalPositionsTimelineJsonNodeConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<HistoricalPositionsTimeline> historicalPositionsTimelines;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

}
