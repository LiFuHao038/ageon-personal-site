package cn.ageon.ai;

import cn.ageon.auth.AccountStatus;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiConversationControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired MockMvc mockMvc;
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AiConversationRepository conversationRepository;

    private SiteUser firstUser;
    private SiteUser secondUser;
    private String firstToken;
    private String secondToken;

    @BeforeEach
    void setUp() throws Exception {
        conversationRepository.deleteAll();
        firstUser = approvedUser("ai-crud-first", "ai-crud-first@example.com", "第一用户");
        secondUser = approvedUser("ai-crud-second", "ai-crud-second@example.com", "第二用户");
        firstToken = login(firstUser.getUsername(), "Password123!");
        secondToken = login(secondUser.getUsername(), "Password123!");
    }

    @Test
    void requiresJwtForAiConversationRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/ai/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void returnsCurrentUsersDailyQuota() throws Exception {
        mockMvc.perform(get("/api/v1/ai/quota")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit", is(20)))
                .andExpect(jsonPath("$.used", is(0)))
                .andExpect(jsonPath("$.remaining", is(20)))
                .andExpect(jsonPath("$.date").isString())
                .andExpect(jsonPath("$.resetsAt").isString());
    }

    @Test
    void createsListsAndLoadsOwnedConversation() throws Exception {
        String created = mockMvc.perform(post("/api/v1/ai/conversations")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("新对话")))
                .andExpect(jsonPath("$.messageCount", is(0)))
                .andReturn().getResponse().getContentAsString();
        long id = readLong(created, "id");

        AiConversation conversation = conversationRepository.findOwnedWithMessages(id, firstUser.getId()).orElseThrow();
        conversation.renameFromFirstMessage("TCP 三次握手");
        conversation.addMessage(AiMessage.user("TCP 为什么需要三次握手？"));
        conversation.addMessage(AiMessage.assistant("用于确认双方的通信能力。"));
        conversationRepository.saveAndFlush(conversation);

        mockMvc.perform(get("/api/v1/ai/conversations")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) id)))
                .andExpect(jsonPath("$[0].messageCount", is(2)));

        mockMvc.perform(get("/api/v1/ai/conversations/{id}", id)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("TCP 三次握手")))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role", is("USER")))
                .andExpect(jsonPath("$.messages[1].role", is("ASSISTANT")));
    }

    @Test
    void hidesConversationFromOtherUsers() throws Exception {
        String created = mockMvc.perform(post("/api/v1/ai/conversations")
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = readLong(created, "id");

        mockMvc.perform(get("/api/v1/ai/conversations/{id}", id)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("AI_CONVERSATION_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/ai/conversations")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void physicallyDeletesOnlyOwnedConversation() throws Exception {
        AiConversation owned = conversationRepository.saveAndFlush(AiConversation.create(firstUser));
        AiConversation foreign = conversationRepository.saveAndFlush(AiConversation.create(secondUser));

        mockMvc.perform(delete("/api/v1/ai/conversations/{id}", foreign.getId())
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/ai/conversations/{id}", owned.getId())
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNoContent());

        assertThat(conversationRepository.findById(owned.getId())).isEmpty();
        assertThat(conversationRepository.findById(foreign.getId())).isPresent();
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

    private static long readLong(String json, String field) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        return root.get(field).asLong();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
