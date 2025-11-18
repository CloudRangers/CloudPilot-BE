package com.cloudrangers.cloudpilot.domain.user.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.Map;

@Converter
public class JsonToMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Map → JSON 저장 (DB 저장용)
     */
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting Map to JSON", e);
        }
    }

    /**
     * JSON → Map 읽기 (Entity 로딩용)
     */
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(
                    dbData,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to Map", e);
        }
    }
}
