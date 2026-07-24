package cn.ageon.admin;

import cn.ageon.auth.*;
import cn.ageon.community.CommunityQuestionRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SiteUserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CommunityQuestionRepository questionRepository;

    private String adminToken;
    private String userToken;
    private SiteUser approvedUser;

    @BeforeEach
    void setUp() throws Exception {
        questionRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.USER)
                .forEach(userRepository::delete);

        approvedUser = SiteUser.pending(
                "社区用户",
                "community-user",
                "community@example.com",
                passwordEncoder.encode("Password123!")
        );
        approvedUser.updateStatus(AccountStatus.APPROVED);
        approvedUser = userRepository.save(approvedUser);

        adminToken = login("ageon-admin", "AgeonAdmin123!");
        userToken = login("community-user", "Password123!");
    }

    @Test
    void approvesPendingUser() throws Exception {
        SiteUser pending = userRepository.save(SiteUser.pending(
                "待审核用户",
                "review-user",
                "review@example.com",
                passwordEncoder.encode("Password123!")
        ));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", pending.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    void moderatesQuestionAndAddsAdminReply() throws Exception {
        String created = mockMvc.perform(post("/api/v1/community/questions")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "如何设计审核流程？",
                                  "detail": "希望问题发布后先由管理员审核。",
                                  "tag": "Java 后端"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus", is("PENDING")))
                .andReturn().getResponse().getContentAsString();

        long questionId = readLong(created, "id");

        mockMvc.perform(get("/api/v1/community/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + questionId + ")]").isEmpty());

        mockMvc.perform(patch("/api/v1/admin/questions/{id}/moderation", questionId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moderationStatus\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus", is("PUBLISHED")));

        mockMvc.perform(get("/api/v1/community/questions/{id}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("如何设计审核流程？")));

        mockMvc.perform(post("/api/v1/admin/questions/{id}/replies", questionId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"审核流程已经接通。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyItems[0].authorRole", is("ADMIN")));
    }

    @Test
    void deletesQuestion() throws Exception {
        String created = mockMvc.perform(post("/api/v1/community/questions")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "待删除问题",
                                  "detail": "管理员应该能够删除问题。",
                                  "tag": "数据库"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        long questionId = readLong(created, "id");

        mockMvc.perform(delete("/api/v1/admin/questions/{id}", questionId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/questions")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + questionId + ")]").isEmpty());
    }

    @Test
    void blocksNormalUserFromAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
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
