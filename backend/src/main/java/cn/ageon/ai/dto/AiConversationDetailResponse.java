package cn.ageon.ai.dto;

import cn.ageon.ai.AiConversation;

import java.time.Instant;
import java.util.List;

public record AiConversationDetailResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<AiMessageResponse> messages
) {
    public static AiConversationDetailResponse from(AiConversation conversation) {
        return new AiConversationDetailResponse(
                conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt(),
                conversation.getMessages().stream().map(AiMessageResponse::from).toList()
        );
    }
}
