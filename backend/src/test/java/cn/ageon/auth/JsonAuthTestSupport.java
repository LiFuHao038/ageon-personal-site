package cn.ageon.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonAuthTestSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonAuthTestSupport() {
    }

    static String extractText(String json, String field) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        return root.get(field).asText();
    }
}
