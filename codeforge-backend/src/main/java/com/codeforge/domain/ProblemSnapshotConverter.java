package com.codeforge.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores an {@link ProblemSnapshot} as JSON in a single column.
 *
 * <p>Its own mapper rather than the application's: that one is configured for
 * the HTTP layer — {@code non_null} inclusion, among other things — and a stored
 * snapshot must round-trip exactly as written, whatever the API's serialisation
 * preferences become later.
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES} is off so that removing a field from the
 * record does not make every round taken before the change unreadable. Adding
 * one is already safe: it arrives null, and only rounds started after the change
 * will carry it.
 */
@Converter
public class ProblemSnapshotConverter
        implements AttributeConverter<ProblemSnapshot, String> {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Override
    public String convertToDatabaseColumn(ProblemSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(snapshot);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialise the interview problem snapshot", e);
        }
    }

    @Override
    public ProblemSnapshot convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ProblemSnapshot.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not read the interview problem snapshot", e);
        }
    }
}
