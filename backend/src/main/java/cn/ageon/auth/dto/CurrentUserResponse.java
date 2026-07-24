package cn.ageon.auth.dto;

import cn.ageon.auth.AccountStatus;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.UserRole;

public record CurrentUserResponse(
        Long id,
        String displayName,
        String username,
        String email,
        UserRole role,
        AccountStatus status
) {
    public static CurrentUserResponse from(SiteUser user) {
        return new CurrentUserResponse(
                user.getId(), user.getDisplayName(), user.getUsername(), user.getEmail(),
                user.getRole(), user.getStatus()
        );
    }
}
