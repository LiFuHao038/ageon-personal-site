package cn.ageon.community;

import cn.ageon.community.dto.QuestionResponse;
import cn.ageon.community.dto.ReplyResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class CommunityQuestionMapper {
    private CommunityQuestionMapper() {
    }

    public static QuestionResponse toResponse(CommunityQuestion question) {
        List<ReplyResponse> replyItems = question.getReplies().stream()
                .map(reply -> new ReplyResponse(
                        reply.getId(), reply.getAuthor(), reply.getAuthorRole(), reply.getContent(), reply.getCreatedAt()))
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getDetail(),
                question.getTag(),
                question.getAuthor(),
                question.getModerationStatus(),
                question.getReplies().size(),
                question.getStatus().label(),
                formatRelativeTime(question.getCreatedAt()),
                question.getLikes(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                replyItems
        );
    }

    private static String formatRelativeTime(Instant createdAt) {
        Duration duration = Duration.between(createdAt, Instant.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + " 分钟前";
        long hours = duration.toHours();
        if (hours < 24) return hours + " 小时前";
        long days = duration.toDays();
        if (days == 1) return "昨天";
        return days + " 天前";
    }
}
