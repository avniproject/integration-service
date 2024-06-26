package org.avni_integration_service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ObjectJsonMapper {
    private static final Logger logger = Logger.getLogger(ObjectJsonMapper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String writeValueAsString(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(String json, Class<T> klass) {
        try {
            return objectMapper.readValue(json, klass);
        } catch (JsonProcessingException e) {
            logger.error(json);
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(File jsonFile, Class<T> klass) {
        try {
            return objectMapper.readValue(jsonFile, klass);
        } catch (Exception e) {
            logger.error(jsonFile);
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(String json, TypeReference typeReference) {
        try {
            return (T) objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(InputStream stream, Class<Map> typeReference) {
        try {
            return (T) objectMapper.readValue(stream, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
