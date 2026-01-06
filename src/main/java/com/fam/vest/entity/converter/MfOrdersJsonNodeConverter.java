package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.dto.response.MFOrderDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class MfOrdersJsonNodeConverter implements AttributeConverter<List<MFOrderDetails>, String> {

    private final Gson gson;

    @Autowired
    public MfOrdersJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<MFOrderDetails> mfOrders) {
        return gson.toJson(mfOrders);
    }

    @Override
    public List<MFOrderDetails> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<MFOrderDetails>>() {}.getType();
        return gson.fromJson(dbData, listType);
    }
}
