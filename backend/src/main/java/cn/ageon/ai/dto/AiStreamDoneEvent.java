package cn.ageon.ai.dto;

public record AiStreamDoneEvent(Long messageId, Long conversationId, String title) {
}
