package cn.ageon.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
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
