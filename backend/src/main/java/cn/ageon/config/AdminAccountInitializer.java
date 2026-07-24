package cn.ageon.config;

import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminAccountInitializer implements ApplicationRunner {
    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminAccountInitializer(
            SiteUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ageon.admin.username}") String username,
            @Value("${ageon.admin.email}") String email,
            @Value("${ageon.admin.password}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase(username)) return;
        userRepository.save(SiteUser.administrator(username, email, passwordEncoder.encode(password)));
    }
}
