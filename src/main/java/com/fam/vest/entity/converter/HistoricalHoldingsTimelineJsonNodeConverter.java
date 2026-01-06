package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.pojo.HistoricalHoldingsTimeline;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class HistoricalHoldingsTimelineJsonNodeConverter implements AttributeConverter<List<HistoricalHoldingsTimeline>, String> {

    private final Gson gson;

    @Autowired
    public HistoricalHoldingsTimelineJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<HistoricalHoldingsTimeline> historicalHoldingsTimelines) {
        return gson.toJson(historicalHoldingsTimelines);
    }

    @Override
    public List<HistoricalHoldingsTimeline> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<HistoricalHoldingsTimeline>>() {}.getType();
        return gson.fromJson(dbData, listType);
    }
}
