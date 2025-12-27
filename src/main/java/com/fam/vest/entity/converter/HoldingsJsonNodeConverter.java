package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.dto.response.HoldingDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class HoldingsJsonNodeConverter implements AttributeConverter<List<HoldingDetails>, String> {

    private final Gson gson;

    @Autowired
    public HoldingsJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<HoldingDetails> holdings) {
        return gson.toJson(holdings);
    }

    @Override
    public List<HoldingDetails> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<HoldingDetails>>() {}.getType();
        return new Gson().fromJson(dbData, listType);
    }
}
