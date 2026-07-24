package cn.ageon.ai;

import cn.ageon.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class HttpAiModelClient implements AiModelClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAiModelClient.class);
    private static final int MAX_ERROR_SUMMARY_LENGTH = 500;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final KimiStreamParser parser;
    private final HttpClient httpClient;

    public HttpAiModelClient(
            AiProperties properties,
            ObjectMapper objectMapper,
            KimiStreamParser parser
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public void assertConfigured() {
        if (properties.getApiKey().isBlank()
                || properties.getPrimaryModel().isBlank()
                || properties.getContextWindowTokens() <= properties.getMaxOutputTokens()) {
            throw new ApiException(
                    "AI_MODEL_NOT_CONFIGURED",
                    "模型服务尚未正确配置，请联系管理员",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    @Override
    public void stream(
            String model,
            List<KimiChatMessage> messages,
            KimiDeltaHandler deltaHandler
    ) {
        assertConfigured();
        if (model == null || model.isBlank()) {
            throw new ApiException(
                    "AI_MODEL_NOT_CONFIGURED",
                    "模型服务尚未正确配置，请联系管理员",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages,
                    "stream", true,
                    "max_tokens", properties.getMaxOutputTokens()
            ));
            HttpRequest request = HttpRequest.newBuilder(completionsUri())
                    .timeout(Duration.ofSeconds(properties.getResponseTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofLines()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                handleErrorResponse(model, response);
            }
            try (java.util.stream.Stream<String> lines = response.body()) {
                var iterator = lines.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    if (!line.startsWith("data:")) continue;
                    KimiStreamChunk chunk = parser.parse(line.substring(5).trim());
                    if (chunk.done()) return;
                    if (!chunk.delta().isEmpty()) deltaHandler.onDelta(chunk.delta());
                }
            }
        } catch (HttpTimeoutException exception) {
            throw AiModelException.timeout(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw AiModelException.connection(exception);
        } catch (UncheckedIOException exception) {
            if (exception.getCause() instanceof HttpTimeoutException timeout) {
                throw AiModelException.timeout(timeout);
            }
            throw AiModelException.connection(exception);
        } catch (IOException exception) {
            throw AiModelException.connection(exception);
        }
    }

    private void handleErrorResponse(
            String model,
            HttpResponse<java.util.stream.Stream<String>> response
    ) {
        String errorBody;
        try (java.util.stream.Stream<String> lines = response.body()) {
            errorBody = lines.limit(20).reduce((left, right) -> left + " " + right).orElse("");
        }
        ErrorDetails details = errorDetails(errorBody);
        log.warn(
                "AI model request failed: model={}, status={}, requestId={}, summary={}",
                model,
                response.statusCode(),
                details.requestId(),
                details.summary()
        );
        throw AiModelException.fromHttpStatus(response.statusCode(), details.requestId());
    }

    private ErrorDetails errorDetails(String errorBody) {
        try {
            JsonNode root = objectMapper.readTree(errorBody);
            String requestId = root.path("request_id").asText("");
            JsonNode error = root.path("error");
            String summary = String.join(
                    " | ",
                    error.path("code").asText(""),
                    error.path("type").asText(""),
                    error.path("message").asText("")
            ).replaceAll("^[ |]+|[ |]+$", "");
            return new ErrorDetails(requestId, summarize(summary));
        } catch (Exception ignored) {
            return new ErrorDetails("", summarize(errorBody));
        }
    }

    private URI completionsUri() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return URI.create(baseUrl + "/chat/completions");
    }

    private static String summarize(String value) {
        String normalized = value.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() <= MAX_ERROR_SUMMARY_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ERROR_SUMMARY_LENGTH) + "...";
    }

    private record ErrorDetails(String requestId, String summary) { }
}
