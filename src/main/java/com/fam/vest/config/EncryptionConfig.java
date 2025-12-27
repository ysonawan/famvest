package com.fam.vest.config;

import com.fam.vest.entity.converter.EncryptionUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${fam.vest.app.secret.enc.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        EncryptionUtils.initializeSecretKey(secretKey);
    }
}
