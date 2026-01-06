package com.fam.vest.entity.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.fam.vest.dto.response.OrderDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Converter
@Component
public class OrdersJsonNodeConverter implements AttributeConverter<List<OrderDetails>, String> {

    private final Gson gson;

    @Autowired
    public OrdersJsonNodeConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convertToDatabaseColumn(List<OrderDetails> orders) {
        return gson.toJson(orders);
    }

    @Override
    public List<OrderDetails> convertToEntityAttribute(String dbData) {
        Type listType = new TypeToken<List<OrderDetails>>() {}.getType();
        return gson.fromJson(dbData, listType);
    }
}
