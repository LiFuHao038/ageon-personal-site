package cn.ageon.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "ageon.cors.allowed-origins=https://gbohmvqgmafa.cloud.sealos.io")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    private static final String FRONTEND_ORIGIN = "https://gbohmvqgmafa.cloud.sealos.io";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SiteUserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearNormalUsers() {
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.USER)
                .forEach(userRepository::delete);
    }

    @Test
    void allowsFrontendLoginPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void registersPendingUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "测试用户",
                                  "username": "new-user",
                                  "email": "new@example.com",
                                  "password": "Password123!",
                                  "acceptedTerms": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.status", is("PENDING")))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void blocksPendingUserLogin() throws Exception {
        userRepository.save(SiteUser.pending(
                "等待审核",
                "pending-user",
                "pending@example.com",
                passwordEncoder.encode("Password123!")
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "pending-user",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCOUNT_PENDING")));
    }

    @Test
    void logsInApprovedUserAndReturnsCurrentUser() throws Exception {
        SiteUser user = SiteUser.pending(
                "已审核用户",
                "approved-user",
                "approved@example.com",
                passwordEncoder.encode("Password123!")
        );
        user.updateStatus(AccountStatus.APPROVED);
        userRepository.save(user);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "approved-user",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("USER")))
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonAuthTestSupport.extractText(response, "token");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("approved-user")));
    }
}
