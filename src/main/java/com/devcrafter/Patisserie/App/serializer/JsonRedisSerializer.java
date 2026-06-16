package com.devcrafter.Patisserie.App.serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.ArrayList;
import java.util.List;

public class JsonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public JsonRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object value)
            throws SerializationException {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException(
                    "Could not serialize: " + e.getMessage(), e
            );
        }
    }

    @Override
    public Object deserialize(byte[] bytes)
            throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            JsonNode node = objectMapper.readTree(bytes);

            if (node.isArray()) {
                List<Object> result = new ArrayList<>();
                for (JsonNode element : node) {
                    // Each element has @class — treeToValue honors it
                    result.add(objectMapper.treeToValue(element, Object.class));
                }
                return result;
            }

            return objectMapper.treeToValue(node, Object.class);

        } catch (Exception e) {
            throw new SerializationException(
                    "Could not deserialize: " + e.getMessage(), e
            );
        }
    }
}
