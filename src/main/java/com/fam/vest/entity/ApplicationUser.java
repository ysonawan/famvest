package com.fam.vest.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "application_user", schema = "app_schema")
public class ApplicationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "user_name", nullable = false, unique = true, length = 255)
    private String userName;

    //this is stored as encrypted password
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "role", length = 255)
    private String role;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;
}
