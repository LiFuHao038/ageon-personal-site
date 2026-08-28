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

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationStatsControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired MockMvc mockMvc;
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        SiteUser user = userRepository.findByUsernameIgnoreCase("apply-stats-user")
                .orElseGet(() -> userRepository.save(SiteUser.pending(
                        "统计测试用户", "apply-stats-user", "apply-stats-user@example.com",
                        passwordEncoder.encode("Password123!"))));
        if (user.getStatus() != AccountStatus.APPROVED) {
            user.updateStatus(AccountStatus.APPROVED);
            userRepository.save(user);
        }
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"apply-stats-user\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = OBJECT_MAPPER.readTree(response).get("token").asText();
    }

    @Test
    void overviewAggregatesAllDimensions() throws Exception {
        long inProgress = createApplication("统计测试公司甲", "后端开发", "互联网", "北京", LocalDate.now().plusDays(2).toString());
        long offerId = createApplication("统计测试公司乙", "Java 开发", "互联网", "上海", null);
        long rejectedId = createApplication("统计测试公司丙", "服务端开发", "国企", "深圳", null);

        changeStatus(offerId, "OFFER");
        changeStatus(rejectedId, "REJECTED");

        mockMvc.perform(get("/api/v1/applications/stats/overview")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(3)))
                .andExpect(jsonPath("$.offers", is(1)))
                .andExpect(jsonPath("$.rejected", is(1)))
                .andExpect(jsonPath("$.active", is(1)))
                .andExpect(jsonPath("$.funnel", hasSize(7)))
                .andExpect(jsonPath("$.funnel[1].status", is("APPLIED")))
                .andExpect(jsonPath("$.funnel[1].reached", is(3)))
                .andExpect(jsonPath("$.funnel[6].status", is("OFFER")))
                .andExpect(jsonPath("$.funnel[6].reached", is(1)))
                .andExpect(jsonPath("$.byCompanyType[0].key", is("互联网")))
                .andExpect(jsonPath("$.byCompanyType[0].total", is(2)))
                .andExpect(jsonPath("$.byCompanyType[0].responseRate", is(0.5)))
                .andExpect(jsonPath("$.byCompanyType[1].key", is("国企")))
                .andExpect(jsonPath("$.upcomingDeadlines[?(@.company == '统计测试公司甲')]").isNotEmpty())
                .andExpect(jsonPath("$.weekly").isArray())
                .andExpect(jsonPath("$.stageDurations").isArray());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/applications/stats/overview"))
                .andExpect(status().isUnauthorized());
    }

    private long createApplication(String company, String position, String companyType,
                                   String city, String deadlineAt) throws Exception {
        String payload = "{\"company\":\"" + company + "\",\"position\":\"" + position
                + "\",\"companyType\":\"" + companyType + "\",\"city\":\"" + city
                + "\",\"appliedAt\":\"" + LocalDate.now() + "\""
                + (deadlineAt == null ? "" : ",\"deadlineAt\":\"" + deadlineAt + "\"")
                + "}";
        String created = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readTree(created).get("id").asLong();
    }

    private void changeStatus(long id, String newStatus) throws Exception {
        mockMvc.perform(post("/api/v1/applications/{id}/status", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + newStatus + "\"}"))
                .andExpect(status().isOk());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}