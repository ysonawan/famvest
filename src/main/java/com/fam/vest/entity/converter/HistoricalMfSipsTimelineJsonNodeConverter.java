package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.pojo.HistoricalMfSipsTimeline;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class HistoricalMfSipsTimelineJsonNodeConverter implements AttributeConverter<List<HistoricalMfSipsTimeline>, String> {

    private final Gson gson;

    @Autowired
    public HistoricalMfSipsTimelineJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<HistoricalMfSipsTimeline> historicalMfSipsTimelines) {
        return gson.toJson(historicalMfSipsTimelines);
    }

    @Override
    public List<HistoricalMfSipsTimeline> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<HistoricalMfSipsTimeline>>() {}.getType();
        return new Gson().fromJson(dbData, listType);
    }
}
