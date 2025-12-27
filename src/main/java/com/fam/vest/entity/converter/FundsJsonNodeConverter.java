package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.dto.response.FundDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class FundsJsonNodeConverter implements AttributeConverter<List<FundDetails>, String> {

    private final Gson gson;

    @Autowired
    public FundsJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<FundDetails> funds) {
        return gson.toJson(funds);
    }

    @Override
    public List<FundDetails> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<FundDetails>>() {}.getType();
        return new Gson().fromJson(dbData, listType);
    }
}
