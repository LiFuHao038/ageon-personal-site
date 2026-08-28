package cn.ageon.apply;

import cn.ageon.apply.dto.ApplicationEventResponse;
import cn.ageon.apply.dto.ApplicationResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class ApplicationMapper {
    private ApplicationMapper() {
    }

    public static ApplicationResponse toResponse(JobApplication application) {
        LocalDate today = LocalDate.now();
        Long daysSinceApplied = application.getAppliedAt() == null ? null
                : ChronoUnit.DAYS.between(application.getAppliedAt(), today);
        Long daysToDeadline = application.getDeadlineAt() == null ? null
                : ChronoUnit.DAYS.between(today, application.getDeadlineAt());
        List<ApplicationEventResponse> events = application.getEvents().stream()
                .map(ApplicationMapper::toEventResponse)
                .toList();

        return new ApplicationResponse(
                application.getId(),
                application.getCompany(),
                application.getPosition(),
                application.getCity(),
                application.getCompanyType(),
                application.getChannel(),
                application.getStatus(),
                application.getStatus().label(),
                application.getSourceUrl(),
                application.getSourceTitle(),
                application.getSourceLogoUrl(),
                application.getSourceError(),
                application.getSourceFetchedAt(),
                application.getDeadlineAt(),
                application.getAppliedAt(),
                application.getNote(),
                daysSinceApplied,
                daysToDeadline,
                application.getCreatedAt(),
                application.getUpdatedAt(),
                events
        );
    }

    public static ApplicationEventResponse toEventResponse(JobApplicationEvent event) {
        ApplicationStatus from = event.getFromStatus();
        return new ApplicationEventResponse(
                event.getId(),
                from,
                from == null ? null : from.label(),
                event.getToStatus(),
                event.getToStatus().label(),
                event.getOccurredAt(),
                event.getNote()
        );
    }
}
