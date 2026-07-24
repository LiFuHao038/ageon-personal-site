package cn.ageon.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTestSupport() {
    }

    static Long extractLong(String json, String field) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return node.get(field).asLong();
    }
}
