package com.fam.vest.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Converter
@Configuration
public class EncryptDecryptConverter implements AttributeConverter<String, String> {

    private final EncryptionUtils encryptionUtils;

    @Autowired
    public EncryptDecryptConverter(EncryptionUtils encryptionUtils) {
        this.encryptionUtils = encryptionUtils;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? encryptionUtils.encrypt(attribute) : null;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData != null ? encryptionUtils.decrypt(dbData) : null;
    }
}
