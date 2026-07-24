package cn.ageon.auth;

import cn.ageon.auth.dto.*;
import cn.ageon.common.ApiException;
import cn.ageon.config.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SiteUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException("USERNAME_EXISTS", "用户名已被使用", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException("EMAIL_EXISTS", "邮箱已被使用", HttpStatus.CONFLICT);
        }
        SiteUser user = SiteUser.pending(
                request.displayName(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);
        return new AuthResponse(null, CurrentUserResponse.from(user), "注册成功，等待管理员审核");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        SiteUser user = userRepository.findByUsernameIgnoreCase(request.identifier())
                .or(() -> userRepository.findByEmailIgnoreCase(request.identifier()))
                .orElseThrow(() -> invalidCredentials());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() == AccountStatus.PENDING) {
            throw new ApiException("ACCOUNT_PENDING", "账号正在等待管理员审核", HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == AccountStatus.REJECTED) {
            throw new ApiException("ACCOUNT_REJECTED", "账号审核未通过", HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new ApiException("ACCOUNT_DISABLED", "账号已被停用", HttpStatus.FORBIDDEN);
        }
        return new AuthResponse(jwtService.createToken(user), CurrentUserResponse.from(user), "登录成功");
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse current(Authentication authentication) {
        return CurrentUserResponse.from(AuthenticatedUser.require(authentication));
    }

    private ApiException invalidCredentials() {
        return new ApiException("INVALID_CREDENTIALS", "账号或密码错误", HttpStatus.UNAUTHORIZED);
    }
}
