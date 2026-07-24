package cn.ageon.ai.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AiQuotaResponse(
        LocalDate date,
        int dailyLimit,
        int used,
        int remaining,
        OffsetDateTime resetsAt
) {
}
