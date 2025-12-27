package com.fam.vest.entity;

import com.fam.vest.entity.converter.EncryptDecryptConverter;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "trading_account", schema = "app_schema")
public class TradingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "user_id", nullable = false, unique = true, length = 255)
    private String userId;

    @Column(name = "password", length = 255)
    @Convert(converter = EncryptDecryptConverter.class)
    private String password;

    @Column(name = "totp_key", length = 255)
    @Convert(converter = EncryptDecryptConverter.class)
    private String totpKey;

    @Column(name = "api_key", length = 255)
    @Convert(converter = EncryptDecryptConverter.class)
    private String apiKey;

    @Column(name = "api_secret", length = 255)
    @Convert(converter = EncryptDecryptConverter.class)
    private String apiSecret;

    @Column(name = "request_token", length = 255)
    @Convert(converter = EncryptDecryptConverter.class)
    private String requestToken;

    @Column(name = "enc_token", length = 255)
    @Convert(converter = EncryptDecryptConverter.class)
    private String encToken;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

    @Column(name = "is_active")
    private Boolean isActive;

}
