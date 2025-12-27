package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.pojo.HistoricalPositionsTimeline;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class HistoricalPositionsTimelineJsonNodeConverter implements AttributeConverter<List<HistoricalPositionsTimeline>, String> {

    private final Gson gson;

    @Autowired
    public HistoricalPositionsTimelineJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<HistoricalPositionsTimeline> historicalPositionsTimelines) {
        return gson.toJson(historicalPositionsTimelines);
    }

    @Override
    public List<HistoricalPositionsTimeline> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<HistoricalPositionsTimeline>>() {}.getType();
        return new Gson().fromJson(dbData, listType);
    }
}
