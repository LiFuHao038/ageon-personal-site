package cn.ageon.ai.dto;

import cn.ageon.ai.AiConversation;

import java.time.Instant;

public record AiConversationSummaryResponse(
        Long id,
        String title,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static AiConversationSummaryResponse from(AiConversation conversation) {
        return new AiConversationSummaryResponse(
                conversation.getId(), conversation.getTitle(), conversation.getMessages().size(),
                conversation.getCreatedAt(), conversation.getUpdatedAt()
        );
    }
}
