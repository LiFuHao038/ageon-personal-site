package cn.ageon.community.dto;

import cn.ageon.auth.UserRole;
import java.time.Instant;

public record ReplyResponse(
        Long id,
        String author,
        UserRole authorRole,
        String content,
        Instant createdAt
) {
}
