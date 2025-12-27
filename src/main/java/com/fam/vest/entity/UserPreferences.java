package com.fam.vest.entity;

import com.fam.vest.enums.DEFAULT_USER_PREFERENCES;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "user_preferences", schema = "app_schema")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "preference", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private DEFAULT_USER_PREFERENCES preference;

    @Column(name = "value")
    private String value;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

    @Transient
    private String displayName;

    @Transient
    private String description;

    @Transient
    private String[] allowedValues;
}
