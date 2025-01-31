package org.avni_integration_service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ObjectJsonMapper {
    private static final Logger logger = Logger.getLogger(ObjectJsonMapper.class);
    public static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JodaModule())
            .registerModule(new ParameterNamesModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    public static String writeValueAsString(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String writeQueryParameterAsEncodedString(Map<String, Object> params){
        Map<String,String> encodedMap = new HashMap<>(params.size());
        for(Map.Entry<String, Object> entry : params.entrySet()){
            try {
                String encodedKey = URLEncoder.encode(entry.getKey(), "UTF-8");
                String encodedValue = URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8");
                encodedMap.put(encodedKey, encodedValue);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return writeValueAsString(encodedMap);
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
