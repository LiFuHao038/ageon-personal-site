package cn.ageon.apply.dto;

import cn.ageon.apply.ApplicationStatus;

import java.time.Instant;

public record ApplicationEventResponse(
        Long id,
        ApplicationStatus fromStatus,
        String fromStatusLabel,
        ApplicationStatus toStatus,
        String toStatusLabel,
        Instant occurredAt,
        String note
) {
}
