package com.fam.vest.entity;

import com.fam.vest.enums.SCHEDULER;
import com.fam.vest.util.CommonUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scheduled_task", schema = "app_schema")
public class ScheduledTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduler_name", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private SCHEDULER schedulerName;

    @Column(name = "description")
    private String description;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_execution_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date lastExecutionDate;

    @Column(name = "execution_start_time", nullable = false)
    private Date executionStartTime;

    @Column(name = "execution_end_time")
    private Date executionEndTime;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Transient
    private String cronDescription;

    public String getCronDescription() {
        return CommonUtil.describeCron(this.cronExpression);
    }
}
