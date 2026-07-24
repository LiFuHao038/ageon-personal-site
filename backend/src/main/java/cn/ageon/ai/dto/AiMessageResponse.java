package cn.ageon.ai.dto;

import cn.ageon.ai.AiMessage;
import cn.ageon.ai.AiMessageRole;
import cn.ageon.ai.AiMessageStatus;

import java.time.Instant;

public record AiMessageResponse(
        Long id,
        AiMessageRole role,
        AiMessageStatus status,
        String content,
        Instant createdAt
) {
    public static AiMessageResponse from(AiMessage message) {
        return new AiMessageResponse(
                message.getId(), message.getRole(), message.getStatus(),
                message.getContent(), message.getCreatedAt()
        );
    }
}
