package cn.ageon.community.dto;

import cn.ageon.community.ModerationStatus;
import java.time.Instant;
import java.util.List;

public record QuestionResponse(
        Long id,
        String title,
        String detail,
        String tag,
        String author,
        ModerationStatus moderationStatus,
        int replies,
        String status,
        String time,
        int likes,
        Instant createdAt,
        Instant updatedAt,
        List<ReplyResponse> replyItems
) {
}
