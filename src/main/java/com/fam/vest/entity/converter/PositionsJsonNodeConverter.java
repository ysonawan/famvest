package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.dto.response.PositionDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class PositionsJsonNodeConverter implements AttributeConverter<List<PositionDetails>, String> {

    private final Gson gson;

    @Autowired
    public PositionsJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<PositionDetails> positions) {
        return gson.toJson(positions);
    }

    @Override
    public List<PositionDetails> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<PositionDetails>>() {}.getType();
        return new Gson().fromJson(dbData, listType);
    }
}
