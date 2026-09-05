package com.invitation.backend.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            List<String> cleaned = attribute.stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            log.error("Error converting List<String> to JSON string", e);
            return String.join(",", attribute);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String trimmed = dbData.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                List<String> list = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                return list != null ? list : new ArrayList<>();
            } catch (Exception e) {
                log.warn("Failed to parse JSON string list: {}", trimmed, e);
            }
        }
        if (trimmed.contains(",")) {
            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(Collections.singletonList(trimmed));
    }
}
