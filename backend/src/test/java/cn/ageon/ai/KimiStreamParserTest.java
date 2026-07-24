package cn.ageon.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KimiStreamParserTest {
    private final KimiStreamParser parser = new KimiStreamParser(new ObjectMapper());

    @Test
    void extractsAssistantDeltaFromMoonshotData() {
        KimiStreamChunk chunk = parser.parse("{\"choices\":[{\"delta\":{\"content\":\"TCP\"}}]}");

        assertThat(chunk.delta()).isEqualTo("TCP");
        assertThat(chunk.done()).isFalse();
    }

    @Test
    void recognizesDoneAndIgnoresMetadataOnlyChunks() {
        assertThat(parser.parse("[DONE]").done()).isTrue();
        assertThat(parser.parse("{\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}").delta()).isEmpty();
    }

    @Test
    void rejectsMalformedUpstreamDataWithoutExposingBody() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOfSatisfying(AiModelException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_MODEL_INVALID_RESPONSE"));
    }
}
