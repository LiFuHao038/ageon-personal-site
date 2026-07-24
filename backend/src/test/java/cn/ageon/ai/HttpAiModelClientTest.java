package cn.ageon.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpAiModelClientTest {
    private static final String PRIVATE_PROMPT = "private-user-prompt";

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void classifiesRateLimitAsFallbackEligibleWithoutExposingPrompt() throws Exception {
        startServer(429, """
                {"error":{"message":"engine overloaded","code":"EngineOverloadedError"},
                 "request_id":"req-429"}
                """);

        HttpAiModelClient client = client();

        assertThatThrownBy(() -> client.stream(
                "qwen-plus",
                List.of(new KimiChatMessage("user", PRIVATE_PROMPT)),
                ignored -> { }
        )).isInstanceOfSatisfying(AiModelException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("AI_MODEL_BUSY");
            assertThat(exception.isFallbackEligible()).isTrue();
            assertThat(exception.getHttpStatus()).isEqualTo(429);
            assertThat(exception.getRequestId()).isEqualTo("req-429");
            assertThat(exception.getMessage()).doesNotContain(PRIVATE_PROMPT);
        });
    }

    @Test
    void classifiesAuthenticationFailureAsNonFallbackError() throws Exception {
        startServer(401, """
                {"error":{"message":"invalid api key"},"request_id":"req-401"}
                """);

        assertThatThrownBy(() -> client().stream(
                "qwen-plus",
                List.of(new KimiChatMessage("user", PRIVATE_PROMPT)),
                ignored -> { }
        )).isInstanceOfSatisfying(AiModelException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("AI_MODEL_AUTH_ERROR");
            assertThat(exception.isFallbackEligible()).isFalse();
            assertThat(exception.getHttpStatus()).isEqualTo(401);
        });
    }

    @Test
    void forwardsStreamingDeltas() throws Exception {
        startServer(200, """
                data: {"choices":[{"delta":{"content":"TCP"}}]}

                data: {"choices":[{"delta":{"content":" works"}}]}

                data: [DONE]

                """);
        List<String> deltas = new ArrayList<>();

        client().stream(
                "qwen-plus",
                List.of(new KimiChatMessage("user", "hello")),
                deltas::add
        );

        assertThat(deltas).containsExactly("TCP", " works");
    }

    private HttpAiModelClient client() {
        ObjectMapper objectMapper = new ObjectMapper();
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        return new HttpAiModelClient(
                properties,
                objectMapper,
                new KimiStreamParser(objectMapper)
        );
    }

    private void startServer(int status, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(
                    "Content-Type",
                    status == 200 ? "text/event-stream" : "application/json"
            );
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }
}
