package cn.ageon.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AiModelGateway {
    private static final Logger log = LoggerFactory.getLogger(AiModelGateway.class);

    private final AiModelClient client;
    private final AiProperties properties;

    public AiModelGateway(AiModelClient client, AiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public void assertConfigured() {
        client.assertConfigured();
    }

    public void stream(
            List<KimiChatMessage> messages,
            AiModelStatusHandler statusHandler,
            KimiDeltaHandler deltaHandler
    ) {
        AtomicBoolean primaryEmitted = new AtomicBoolean(false);
        try {
            client.stream(properties.getPrimaryModel(), messages, delta -> {
                if (!delta.isEmpty()) primaryEmitted.set(true);
                deltaHandler.onDelta(delta);
            });
        } catch (AiModelException exception) {
            if (!shouldFallback(exception, primaryEmitted.get())) throw exception;

            String fallbackModel = properties.getFallbackModel();
            log.warn(
                    "AI fallback triggered: model={}, status={}, fallback=true, requestId={}, summary={}",
                    properties.getPrimaryModel(),
                    exception.getHttpStatus(),
                    exception.getRequestId(),
                    exception.getCode()
            );
            statusHandler.onFallback(fallbackModel);
            client.stream(fallbackModel, messages, deltaHandler);
        }
    }

    private boolean shouldFallback(AiModelException exception, boolean primaryEmitted) {
        return exception.isFallbackEligible()
                && !primaryEmitted
                && properties.isFallbackEnabled()
                && !properties.getFallbackModel().isBlank()
                && !properties.getFallbackModel().equals(properties.getPrimaryModel());
    }
}
