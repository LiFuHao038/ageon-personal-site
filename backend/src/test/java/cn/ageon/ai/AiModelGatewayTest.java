package cn.ageon.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AiModelGatewayTest {
    private final AiModelClient client = mock(AiModelClient.class);
    private final AiProperties properties = new AiProperties();
    private final List<KimiChatMessage> messages = List.of(
            new KimiChatMessage("user", "explain TCP")
    );

    private AiModelGateway gateway;

    @BeforeEach
    void setUp() {
        properties.setPrimaryModel("qwen-plus");
        properties.setFallbackEnabled(true);
        properties.setFallbackModel("kimi/kimi-k3");
        gateway = new AiModelGateway(client, properties);
    }

    @Test
    void usesPrimaryWithoutCallingFallbackWhenPrimarySucceeds() {
        doAnswer(invocation -> {
            KimiDeltaHandler handler = invocation.getArgument(2);
            handler.onDelta("primary");
            return null;
        }).when(client).stream(eq("qwen-plus"), anyList(), any(KimiDeltaHandler.class));
        List<String> deltas = new ArrayList<>();
        List<String> statuses = new ArrayList<>();

        gateway.stream(messages, statuses::add, deltas::add);

        assertThat(deltas).containsExactly("primary");
        assertThat(statuses).isEmpty();
        verify(client, never()).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));
    }

    @Test
    void switchesToFallbackWhenPrimaryReturns429BeforeFirstToken() {
        stubPrimaryFailure(AiModelException.fromHttpStatus(429, "req-primary"));
        stubFallbackSuccess("fallback");
        List<String> statuses = new ArrayList<>();
        List<String> deltas = new ArrayList<>();

        gateway.stream(messages, statuses::add, deltas::add);

        assertThat(statuses).containsExactly("kimi/kimi-k3");
        assertThat(deltas).containsExactly("fallback");
    }

    @Test
    void switchesToFallbackWhenPrimaryReturns503BeforeFirstToken() {
        stubPrimaryFailure(AiModelException.fromHttpStatus(503, "req-primary"));
        stubFallbackSuccess("fallback");

        gateway.stream(messages, ignored -> { }, ignored -> { });

        verify(client).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));
    }

    @Test
    void switchesToFallbackWhenPrimaryTimesOutBeforeFirstToken() {
        stubPrimaryFailure(AiModelException.timeout(null));
        stubFallbackSuccess("fallback");

        gateway.stream(messages, ignored -> { }, ignored -> { });

        verify(client).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));
    }

    @Test
    void doesNotSwitchForAuthenticationFailure() {
        stubPrimaryFailure(AiModelException.fromHttpStatus(401, "req-primary"));

        assertThatThrownBy(() -> gateway.stream(messages, ignored -> { }, ignored -> { }))
                .isInstanceOfSatisfying(AiModelException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_MODEL_AUTH_ERROR"));
        verify(client, never()).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));
    }

    @Test
    void doesNotSwitchAfterPrimaryEmitsFirstToken() {
        doAnswer(invocation -> {
            KimiDeltaHandler handler = invocation.getArgument(2);
            handler.onDelta("partial");
            throw AiModelException.fromHttpStatus(429, "req-primary");
        }).when(client).stream(eq("qwen-plus"), anyList(), any(KimiDeltaHandler.class));
        List<String> deltas = new ArrayList<>();

        assertThatThrownBy(() -> gateway.stream(messages, ignored -> { }, deltas::add))
                .isInstanceOf(AiModelException.class);
        assertThat(deltas).containsExactly("partial");
        verify(client, never()).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));
    }

    @Test
    void returnsFallbackFailureWhenBothModelsFail() {
        stubPrimaryFailure(AiModelException.fromHttpStatus(429, "req-primary"));
        doThrow(AiModelException.fromHttpStatus(503, "req-fallback"))
                .when(client).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));

        assertThatThrownBy(() -> gateway.stream(messages, ignored -> { }, ignored -> { }))
                .isInstanceOfSatisfying(AiModelException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(503);
                    assertThat(exception.getRequestId()).isEqualTo("req-fallback");
                });
    }

    private void stubPrimaryFailure(AiModelException exception) {
        doThrow(exception).when(client)
                .stream(eq("qwen-plus"), anyList(), any(KimiDeltaHandler.class));
    }

    private void stubFallbackSuccess(String delta) {
        doAnswer(invocation -> {
            KimiDeltaHandler handler = invocation.getArgument(2);
            handler.onDelta(delta);
            return null;
        }).when(client).stream(eq("kimi/kimi-k3"), anyList(), any(KimiDeltaHandler.class));
    }
}
