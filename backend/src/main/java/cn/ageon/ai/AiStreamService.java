package cn.ageon.ai;

import cn.ageon.ai.dto.AiQuotaResponse;
import cn.ageon.ai.dto.AiModelStatusEvent;
import cn.ageon.ai.dto.AiStreamDoneEvent;
import cn.ageon.ai.dto.AiStreamErrorEvent;
import cn.ageon.ai.dto.AiStreamMessageEvent;
import cn.ageon.auth.SiteUser;
import cn.ageon.common.ApiException;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiStreamService {
    private static final long SSE_TIMEOUT_MILLIS = 120_000L;

    private final AiModelGateway modelGateway;
    private final AiProperties aiProperties;
    private final AiChatPersistenceService persistenceService;
    private final AiQuotaService quotaService;
    private final Set<Long> activeUsers = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AiStreamService(
            AiModelGateway modelGateway,
            AiProperties aiProperties,
            AiChatPersistenceService persistenceService,
            AiQuotaService quotaService
    ) {
        this.modelGateway = modelGateway;
        this.aiProperties = aiProperties;
        this.persistenceService = persistenceService;
        this.quotaService = quotaService;
    }

    public SseEmitter start(Long conversationId, SiteUser user, String rawContent) {
        modelGateway.assertConfigured();
        String content = validate(rawContent);
        List<KimiChatMessage> history = persistenceService.loadHistory(conversationId, user.getId());
        if (!activeUsers.add(user.getId())) {
            throw new ApiException(
                    "AI_REQUEST_IN_PROGRESS",
                    "已有回答正在生成，请稍后再试",
                    HttpStatus.CONFLICT
            );
        }

        AiQuotaReservation reservation = null;
        try {
            KimiPromptBuilder promptBuilder = new KimiPromptBuilder(
                    aiProperties.getContextWindowTokens(), aiProperties.getMaxOutputTokens()
            );
            List<KimiChatMessage> prompt = promptBuilder.build(history, content);
            reservation = quotaService.reserve(user.getId());
            persistenceService.saveUserMessage(conversationId, user.getId(), content);

            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
            StreamExecution execution = new StreamExecution(
                    conversationId, user.getId(), reservation, emitter
            );
            registerCallbacks(execution);
            Future<?> future = executor.submit(() -> runStream(execution, prompt));
            execution.future().set(future);
            return emitter;
        } catch (RuntimeException exception) {
            if (reservation != null) quotaService.release(reservation);
            activeUsers.remove(user.getId());
            throw exception;
        }
    }

    private void runStream(StreamExecution execution, List<KimiChatMessage> prompt) {
        StringBuilder answer = new StringBuilder();
        try {
            modelGateway.stream(
                    prompt,
                    model -> send(
                            execution.emitter(),
                            "model_status",
                            new AiModelStatusEvent("fallback", model)
                    ),
                    delta -> {
                        if (delta.isEmpty()) return;
                        answer.append(delta);
                        send(execution.emitter(), "message", new AiStreamMessageEvent(delta));
                    }
            );
            if (answer.isEmpty()) throw AiModelException.connection(null);

            AiMessage assistant = persistenceService.saveAssistantMessage(
                    execution.conversationId(), execution.userId(), answer.toString()
            );
            execution.completedMessageId().set(assistant.getId());
            AiQuotaResponse quota = quotaService.current(execution.userId());
            String title = persistenceService.getTitle(execution.conversationId(), execution.userId());
            send(execution.emitter(), "quota", quota);
            send(execution.emitter(), "done", new AiStreamDoneEvent(
                    assistant.getId(), execution.conversationId(), title
            ));
            if (execution.terminal().compareAndSet(false, true)) {
                activeUsers.remove(execution.userId());
                execution.emitter().complete();
            }
        } catch (RuntimeException exception) {
            fail(execution, localFailure(exception), true);
        }
    }

    private void registerCallbacks(StreamExecution execution) {
        execution.emitter().onCompletion(() -> fail(execution, disconnected(), false));
        execution.emitter().onTimeout(() -> fail(execution, AiModelException.timeout(null), false));
        execution.emitter().onError(error -> fail(execution, disconnected(), false));
    }

    private void fail(StreamExecution execution, AiModelException failure, boolean sendError) {
        if (!execution.terminal().compareAndSet(false, true)) return;
        Future<?> future = execution.future().get();
        if (future != null && !future.isDone()) future.cancel(true);
        activeUsers.remove(execution.userId());
        try {
            persistenceService.saveFailedAttempt(
                    execution.conversationId(), execution.userId(),
                    execution.completedMessageId().get(), failure.getMessage()
            );
        } catch (RuntimeException ignored) {
        }
        try {
            quotaService.release(execution.reservation());
        } catch (RuntimeException ignored) {
        }
        if (sendError) {
            try {
                send(execution.emitter(), "error", new AiStreamErrorEvent(
                        failure.getCode(), failure.getMessage()
                ));
            } catch (RuntimeException ignored) {
            }
        }
        execution.emitter().complete();
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException exception) {
            throw disconnected();
        }
    }

    private static String validate(String content) {
        if (content == null || content.trim().isEmpty() || content.trim().length() > 4_000) {
            throw new ApiException("AI_MESSAGE_INVALID", "Message must be non-empty and at most 4000 characters.", HttpStatus.BAD_REQUEST);
        }
        return content.trim();
    }

    private static AiModelException localFailure(RuntimeException exception) {
        return exception instanceof AiModelException modelException
                ? modelException
                : AiModelException.connection(exception);
    }

    private static AiModelException disconnected() {
        return new AiModelException(
                "AI_STREAM_DISCONNECTED",
                "请求已取消",
                false,
                null,
                "",
                null
        );
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private record StreamExecution(
            Long conversationId,
            Long userId,
            AiQuotaReservation reservation,
            SseEmitter emitter,
            AtomicBoolean terminal,
            AtomicReference<Future<?>> future,
            AtomicReference<Long> completedMessageId
    ) {
        private StreamExecution(
                Long conversationId, Long userId, AiQuotaReservation reservation, SseEmitter emitter
        ) {
            this(conversationId, userId, reservation, emitter, new AtomicBoolean(),
                    new AtomicReference<>(), new AtomicReference<>());
        }
    }
}
