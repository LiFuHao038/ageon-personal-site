package cn.ageon.admin.dto;

public record AdminOverviewResponse(
        long pendingUsers,
        long pendingQuestions,
        long publishedQuestions,
        long totalReplies
) {
}
