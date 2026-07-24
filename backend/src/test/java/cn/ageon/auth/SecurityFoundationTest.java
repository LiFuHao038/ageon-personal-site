package cn.ageon.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityFoundationTest {
    @Autowired
    SiteUserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void storesPendingUserWithEncodedPassword() {
        String passwordHash = passwordEncoder.encode("Password123!");
        SiteUser user = userRepository.save(SiteUser.pending(
                "测试用户",
                "test-user",
                "test@example.com",
                passwordHash
        ));

        assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
    }
}
