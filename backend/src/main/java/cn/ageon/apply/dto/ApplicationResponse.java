package cn.ageon.apply.dto;

import cn.ageon.apply.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplicationResponse(
        Long id,
        String company,
        String position,
        String city,
        String companyType,
        String channel,
        ApplicationStatus status,
        String statusLabel,
        String sourceUrl,
        String sourceTitle,
        String sourceLogoUrl,
        String sourceError,
        Instant sourceFetchedAt,
        LocalDate deadlineAt,
        LocalDate appliedAt,
        String note,
        Long daysSinceApplied,
        Long daysToDeadline,
        Instant createdAt,
        Instant updatedAt,
        List<ApplicationEventResponse> events
) {
}
