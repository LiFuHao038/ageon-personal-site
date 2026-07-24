package cn.ageon.admin.dto;

import cn.ageon.auth.UserRole;
import java.time.Instant;

public record AdminReplyResponse(
        Long id,
        Long questionId,
        String questionTitle,
        String author,
        UserRole authorRole,
        String content,
        Instant createdAt
) {
}
