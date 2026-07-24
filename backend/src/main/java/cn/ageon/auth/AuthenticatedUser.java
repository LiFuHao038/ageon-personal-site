package cn.ageon.auth;

import cn.ageon.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public final class AuthenticatedUser {
    private AuthenticatedUser() {
    }

    public static SiteUser require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SiteUser user)) {
            throw new ApiException("UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    public static SiteUser requireApprovedUser(Authentication authentication) {
        SiteUser user = require(authentication);
        if (user.getStatus() != AccountStatus.APPROVED) {
            throw new ApiException("ACCOUNT_NOT_APPROVED", "账号尚未通过审核", HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
