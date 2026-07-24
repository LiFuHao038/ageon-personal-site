package cn.ageon.ai;

import cn.ageon.auth.AccountStatus;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
import cn.ageon.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ageon.ai.api-key=test-key",
        "ageon.ai.primary-model=qwen-plus",
        "ageon.ai.fallback-model=kimi/kimi-k3",
        "ageon.ai.context-window-tokens=8192"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiStreamControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired MockMvc mockMvc;
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AiConversationRepository conversationRepository;
    @Autowired AiDailyUsageRepository usageRepository;
    @MockBean AiModelGateway modelGateway;

    private SiteUser firstUser;
    private SiteUser secondUser;
    private String firstToken;
    private String secondToken;

    @BeforeEach
    void setUp() throws Exception {
        usageRepository.deleteAll();
        conversationRepository.deleteAll();
        firstUser = approvedUser("ai-stream-first", "ai-stream-first@example.com", "流式用户一");
        secondUser = approvedUser("ai-stream-second", "ai-stream-second@example.com", "流式用户二");
        firstToken = login(firstUser.getUsername(), "Password123!");
        secondToken = login(secondUser.getUsername(), "Password123!");
    }

    @Test
    void streamsMessageQuotaAndDoneThenPersistsMessages() throws Exception {
        doAnswer(invocation -> {
            AiModelStatusHandler statusHandler = invocation.getArgument(1);
            KimiDeltaHandler handler = invocation.getArgument(2);
            statusHandler.onFallback("kimi/kimi-k3");
            handler.onDelta("TCP");
            handler.onDelta(" 通过三次握手建立连接。");
            return null;
        }).when(modelGateway).stream(
                any(List.class), any(AiModelStatusHandler.class), any(KimiDeltaHandler.class)
        );

        long conversationId = createConversation(firstToken);
        MvcResult started = stream(conversationId, firstToken, "TCP 为什么需要三次握手？")
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:model_status")))
                .andExpect(content().string(containsString("kimi/kimi-k3")))
                .andExpect(content().string(containsString("event:message")))
                .andExpect(content().string(containsString("event:quota")))
                .andExpect(content().string(containsString("event:done")))
                .andExpect(content().string(containsString("TCP")));

        AiConversation conversation = conversationRepository
                .findOwnedWithMessages(conversationId, firstUser.getId()).orElseThrow();
        assertThat(conversation.getTitle()).isEqualTo("TCP 为什么需要三次握手？");
        assertThat(conversation.getMessages()).extracting(AiMessage::getRole)
                .containsExactly(AiMessageRole.USER, AiMessageRole.ASSISTANT);
        assertThat(conversation.getMessages().get(1).getStatus()).isEqualTo(AiMessageStatus.COMPLETED);
        assertThat(usageRepository.findByUserIdAndUsageDate(
                firstUser.getId(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))
                .orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test
    void emitsLocalErrorPersistsFailedAttemptAndReleasesQuota() throws Exception {
        doThrow(AiModelException.timeout(null)).when(modelGateway)
                .stream(any(List.class), any(AiModelStatusHandler.class), any(KimiDeltaHandler.class));

        long conversationId = createConversation(firstToken);
        MvcResult started = stream(conversationId, firstToken, "解释一下 HTTP/2")
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("AI_MODEL_BUSY")));

        AiConversation conversation = conversationRepository
                .findOwnedWithMessages(conversationId, firstUser.getId()).orElseThrow();
        assertThat(conversation.getMessages()).hasSize(2);
        assertThat(conversation.getMessages().get(1).getStatus()).isEqualTo(AiMessageStatus.FAILED);
        assertThat(usageRepository.findByUserIdAndUsageDate(
                firstUser.getId(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))
                .orElseThrow().getUsedCount()).isZero();
    }

    @Test
    void requiresJwtAndHidesAnotherUsersConversation() throws Exception {
        long conversationId = createConversation(secondToken);

        mockMvc.perform(post("/api/v1/ai/conversations/{id}/messages/stream", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        stream(conversationId, firstToken, "test")
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidInputBeforeOpeningStream() throws Exception {
        long conversationId = createConversation(firstToken);

        stream(conversationId, firstToken, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.code", org.hamcrest.Matchers.is("AI_MESSAGE_INVALID")
                ));
    }

    @Test
    void returnsServiceUnavailableWhenAiModelIsNotConfigured() throws Exception {
        doThrow(new ApiException(
                "AI_MODEL_NOT_CONFIGURED", "模型服务尚未配置", HttpStatus.SERVICE_UNAVAILABLE
        )).when(modelGateway).assertConfigured();
        long conversationId = createConversation(firstToken);

        stream(conversationId, firstToken, "解释 TCP")
                .andExpect(status().isServiceUnavailable())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.code", org.hamcrest.Matchers.is("AI_MODEL_NOT_CONFIGURED")
                ));
    }

    @Test
    void rejectsConcurrentStreamsAcrossConversationsForSameUser() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            KimiDeltaHandler handler = invocation.getArgument(2);
            handler.onDelta("done");
            return null;
        }).when(modelGateway).stream(
                any(List.class), any(AiModelStatusHandler.class), any(KimiDeltaHandler.class)
        );
        long firstConversation = createConversation(firstToken);
        long secondConversation = createConversation(firstToken);

        MvcResult first = stream(firstConversation, firstToken, "first")
                .andExpect(request().asyncStarted())
                .andReturn();
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        stream(secondConversation, firstToken, "second")
                .andExpect(status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.code", org.hamcrest.Matchers.is("AI_REQUEST_IN_PROGRESS")
                ));

        release.countDown();
        mockMvc.perform(asyncDispatch(first)).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions stream(
            long conversationId, String token, String content
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/ai/conversations/{id}/messages/stream", conversationId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(OBJECT_MAPPER.writeValueAsString(java.util.Map.of("content", content))));
    }

    private long createConversation(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/ai/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = OBJECT_MAPPER.readTree(response);
        return root.get("id").asLong();
    }

    private SiteUser approvedUser(String username, String email, String displayName) {
        SiteUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> SiteUser.pending(displayName, username, email, passwordEncoder.encode("Password123!")));
        user.updateStatus(AccountStatus.APPROVED);
        return userRepository.saveAndFlush(user);
    }

    private String login(String identifier, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readTree(response).get("token").asText();
    }
}
