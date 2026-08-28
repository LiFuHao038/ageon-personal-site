package cn.ageon.apply.dto;

import java.util.List;

public record StatsOverviewResponse(
        int total,
        int active,
        int offers,
        int rejected,
        List<FunnelStageResponse> funnel,
        List<StageDurationResponse> stageDurations,
        List<GroupStatResponse> byCompanyType,
        List<GroupStatResponse> byCity,
        List<WeeklyPointResponse> weekly,
        List<DeadlineItemResponse> upcomingDeadlines
) {
}