package cn.ageon.community;

import cn.ageon.auth.AccountStatus;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityQuestionControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired MockMvc mockMvc;
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        SiteUser user = userRepository.findByUsernameIgnoreCase("community-contract-user")
                .orElseGet(() -> userRepository.save(SiteUser.pending(
                        "社区测试用户", "community-contract-user", "community-contract@example.com",
                        passwordEncoder.encode("Password123!")
                )));
        if (user.getStatus() != AccountStatus.APPROVED) {
            user.updateStatus(AccountStatus.APPROVED);
            userRepository.save(user);
        }
        userToken = login("community-contract-user", "Password123!");
        adminToken = login("ageon-admin", "AgeonAdmin123!");
    }

    @Test
    void listsPublishedQuestions() throws Exception {
        long id = createPublishedQuestion("可公开浏览的问题");
        mockMvc.perform(get("/api/v1/community/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty())
                .andExpect(jsonPath("$[0].id", is((int) id)))
                .andExpect(jsonPath("$[0].status").exists());
    }

    @Test
    void createsPendingQuestionFromAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/community/questions")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "如何设计社区接口？",
                                  "detail": "我希望前端可以直接消费响应字段。",
                                  "tag": "Java 后端"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("如何设计社区接口？")))
                .andExpect(jsonPath("$.author", is("社区测试用户")))
                .andExpect(jsonPath("$.moderationStatus", is("PENDING")))
                .andExpect(jsonPath("$.replies", is(0)));
    }

    @Test
    void repliesToPublishedQuestionAndMarksAnswered() throws Exception {
        long id = createPublishedQuestion("SSE 如何联调？");
        mockMvc.perform(post("/api/v1/community/questions/{id}/replies", id)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"先固定请求和响应字段，再接前端。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies", is(1)))
                .andExpect(jsonPath("$.status", is("已回复")))
                .andExpect(jsonPath("$.replyItems[0].authorRole", is("USER")));
    }

    @Test
    void likesPublishedQuestion() throws Exception {
        long id = createPublishedQuestion("点赞接口测试");
        mockMvc.perform(post("/api/v1/community/questions/{id}/likes", id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes", greaterThanOrEqualTo(1)));
    }

    private long createPublishedQuestion(String title) throws Exception {
        String created = mockMvc.perform(post("/api/v1/community/questions")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"detail\":\"用于社区接口测试。\",\"tag\":\"AI 应用\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = OBJECT_MAPPER.readTree(created).get("id").asLong();
        mockMvc.perform(patch("/api/v1/admin/questions/{id}/moderation", id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moderationStatus\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());
        return id;
    }

    private String login(String identifier, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readTree(response).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
