package cn.ageon.apply;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired MockMvc mockMvc;
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String userToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        ensureApprovedUser("投递测试用户A", "apply-user-a", "apply-user-a@example.com");
        ensureApprovedUser("投递测试用户B", "apply-user-b", "apply-user-b@example.com");
        userToken = login("apply-user-a");
        otherToken = login("apply-user-b");
    }

    @Test
    void createsApplicationWithAppliedStatusAndInitialEvent() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company": "字节跳动",
                                  "position": "后端开发工程师",
                                  "city": "北京",
                                  "companyType": "互联网",
                                  "channel": "官网",
                                  "appliedAt": "2026-07-20",
                                  "note": "创建接口测试"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company", is("字节跳动")))
                .andExpect(jsonPath("$.position", is("后端开发工程师")))
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.statusLabel", is("已投递")))
                .andExpect(jsonPath("$.daysSinceApplied").isNumber())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].fromStatus").value(nullValue()))
                .andExpect(jsonPath("$.events[0].toStatus", is("APPLIED")))
                .andExpect(jsonPath("$.events[0].toStatusLabel", is("已投递")))
                .andExpect(jsonPath("$.events[0].note", is("创建投递记录")));
    }

    @Test
    void changesStatusAndAppendsEvent() throws Exception {
        long id = createApplication(userToken, "美团", "Java 开发工程师", "2026-07-19");
        mockMvc.perform(post("/api/v1/applications/{id}/status", id)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WRITTEN_TEST\",\"note\":\"收到笔试邀请\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WRITTEN_TEST")))
                .andExpect(jsonPath("$.statusLabel", is("笔试/测评")))
                .andExpect(jsonPath("$.events", hasSize(2)))
                .andExpect(jsonPath("$.events[1].fromStatus", is("APPLIED")))
                .andExpect(jsonPath("$.events[1].toStatus", is("WRITTEN_TEST")))
                .andExpect(jsonPath("$.events[1].note", is("收到笔试邀请")));
    }

    @Test
    void rejectsIllegalStatusTransition() throws Exception {
        long id = createApplication(userToken, "腾讯", "后台开发工程师", "2026-07-18");
        mockMvc.perform(post("/api/v1/applications/{id}/status", id)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_STATUS_TRANSITION")));
    }

    @Test
    void hidesOtherUsersApplication() throws Exception {
        long id = createApplication(userToken, "华为", "通用软件开发工程师", "2026-07-17");
        mockMvc.perform(get("/api/v1/applications/{id}", id)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        mockMvc.perform(delete("/api/v1/applications/{id}", id)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"未登录\",\"position\":\"测试\",\"appliedAt\":\"2026-07-20\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesStatusMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/applications/meta/statuses")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[0].status", is("PREPARING")))
                .andExpect(jsonPath("$[0].label", is("准备投递")))
                .andExpect(jsonPath("$[0].terminal", is(false)))
                .andExpect(jsonPath("$[0].allowed").isArray());
    }

    @Test
    void listsOnlyOwnApplications() throws Exception {
        createApplication(userToken, "阿里巴巴", "后端开发", "2026-07-16");
        createApplication(otherToken, "百度", "后端开发", "2026-07-16");
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.company == '阿里巴巴')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.company == '百度')]").isEmpty());
    }

    @Test
    void deletesApplicationThenReturns404() throws Exception {
        long id = createApplication(userToken, "网易", "服务端开发", "2026-07-15");
        mockMvc.perform(delete("/api/v1/applications/{id}", id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/applications/{id}", id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    private void ensureApprovedUser(String displayName, String username, String email) {
        SiteUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> userRepository.save(SiteUser.pending(
                        displayName, username, email, passwordEncoder.encode("Password123!"))));
        if (user.getStatus() != AccountStatus.APPROVED) {
            user.updateStatus(AccountStatus.APPROVED);
            userRepository.save(user);
        }
    }

    private long createApplication(String token, String company, String position, String appliedAt) throws Exception {
        String created = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"" + company + "\",\"position\":\"" + position
                                + "\",\"appliedAt\":\"" + appliedAt + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readTree(created).get("id").asLong();
    }

    private String login(String identifier) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + identifier + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readTree(response).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
