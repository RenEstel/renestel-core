package io.github.renestel.core.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.List;

public class Jacksonable {

    public static TypeReference<List<ObjectNode>> listObjectNode
            = new TypeReference<>() {
    };

    public static ObjectMapper jackson = new ObjectMapper(new JsonFactory());

    static {
        jackson.registerModule(new JavaTimeModule());
    }

    public static String toJson(Object object) {
        try {
            return jackson.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("fail to json object", e);
        }
    }

    public static String stringArgsToJson(String... args) {
        try {
            return jackson.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("fail to json object", e);
        }
    }

    public static Object fromJson(String json, Class<?> clazz) {
        try {
            return jackson.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("fail to build object from json", e);
        }
    }

    public static ObjectNode fromJson(String json) {
        try {
            return jackson.readValue(json, ObjectNode.class);
        } catch (IOException e) {
            throw new RuntimeException("fail to build object from json", e);
        }
    }

    public static ObjectNode fromJsonSafe(String json) {
        try {
            return jackson.readValue(json, ObjectNode.class);
        } catch (IOException e) {
            try {
                return jackson.readValue("{}", ObjectNode.class);
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException("fail to build object from json", e);
            }
        }
    }

    public static List<ObjectNode> fromJsonToListObjectNode(String json) {
        try {
            return jackson.readValue(json, listObjectNode);
        } catch (IOException e) {
            throw new RuntimeException("fail to build object from json", e);
        }
    }
}
