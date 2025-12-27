package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.pojo.HistoricalFundsTimeline;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class HistoricalFundsTimelineJsonNodeConverter implements AttributeConverter<List<HistoricalFundsTimeline>, String> {

    private final Gson gson;

    @Autowired
    public HistoricalFundsTimelineJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<HistoricalFundsTimeline> historicalFundsTimelines) {
        return gson.toJson(historicalFundsTimelines);
    }

    @Override
    public List<HistoricalFundsTimeline> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<HistoricalFundsTimeline>>() {}.getType();
        return new Gson().fromJson(dbData, listType);
    }
}
