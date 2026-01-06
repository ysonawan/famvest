package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.dto.response.MFSIPDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class MfSipsJsonNodeConverter implements AttributeConverter<List<MFSIPDetails>, String> {

    private final Gson gson;

    @Autowired
    public MfSipsJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<MFSIPDetails> mfSips) {
        return gson.toJson(mfSips);
    }

    @Override
    public List<MFSIPDetails> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<MFSIPDetails>>() {}.getType();
        return gson.fromJson(dbData, listType);
    }
}
