package cn.ageon.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class KimiStreamParser {
    private final ObjectMapper objectMapper;

    public KimiStreamParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KimiStreamChunk parse(String data) {
        if ("[DONE]".equals(data.trim())) return KimiStreamChunk.doneChunk();
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            return KimiStreamChunk.delta(content.isTextual() ? content.asText() : "");
        } catch (Exception exception) {
            throw AiModelException.protocol(exception);
        }
    }
}
