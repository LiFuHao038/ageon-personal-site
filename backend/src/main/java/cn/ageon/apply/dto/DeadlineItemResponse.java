package cn.ageon.apply.dto;

import java.time.LocalDate;

public record DeadlineItemResponse(
        long id,
        String company,
        String position,
        LocalDate deadlineAt,
        long daysLeft,
        boolean overdue
) {
}