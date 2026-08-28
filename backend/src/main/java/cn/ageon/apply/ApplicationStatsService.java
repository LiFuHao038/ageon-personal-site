package cn.ageon.apply;

import cn.ageon.apply.dto.DeadlineItemResponse;
import cn.ageon.apply.dto.FunnelStageResponse;
import cn.ageon.apply.dto.GroupStatResponse;
import cn.ageon.apply.dto.StageDurationResponse;
import cn.ageon.apply.dto.StatsOverviewResponse;
import cn.ageon.apply.dto.WeeklyPointResponse;
import cn.ageon.auth.AuthenticatedUser;
import cn.ageon.auth.SiteUser;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 投递统计：以 {@code job_application_events} 作为「到达漏斗/阶段耗时」的唯一真相来源，
 * 以 {@code job_applications} 当前状态派生汇总数、分组、周趋势与临期提醒。
 * 个人站数据量小，全部在内存中聚合，避免为统计引入复杂的 JPQL 投影。
 */
@Service
public class ApplicationStatsService {
    private static final int DEADLINE_WINDOW_DAYS = 7;
    private static final int DEADLINE_LIMIT = 20;

    private final JobApplicationRepository applicationRepository;
    private final JobApplicationEventRepository eventRepository;

    public ApplicationStatsService(
            JobApplicationRepository applicationRepository,
            JobApplicationEventRepository eventRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public StatsOverviewResponse overview(Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        List<JobApplication> applications = applicationRepository.findByOwnerId(owner.getId(), Sort.unsorted());
        List<JobApplicationEvent> events = eventRepository.findByApplicationOwnerIdOrderByOccurredAtAsc(owner.getId());
        return buildOverview(applications, events);
    }

    StatsOverviewResponse buildOverview(List<JobApplication> applications, List<JobApplicationEvent> events) {
        int total = applications.size();
        int offers = 0;
        int rejected = 0;
        int active = 0;
        for (JobApplication application : applications) {
            ApplicationStatus status = application.getStatus();
            if (status == ApplicationStatus.OFFER) {
                offers++;
            } else if (status == ApplicationStatus.REJECTED) {
                rejected++;
            }
            if (!status.isTerminal()) {
                active++;
            }
        }
        return new StatsOverviewResponse(
                total,
                active,
                offers,
                rejected,
                buildFunnel(events),
                buildStageDurations(applications, events),
                buildGroups(applications, true),
                buildGroups(applications, false),
                buildWeekly(applications),
                buildDeadlines(applications)
        );
    }

    /** 漏斗「到达数」= 曾进入该状态的去重投递数（允许跳级，因此不代表逐级转化率）。 */
    private List<FunnelStageResponse> buildFunnel(List<JobApplicationEvent> events) {
        Map<Long, Set<ApplicationStatus>> reachedByApplication = new HashMap<>();
        for (JobApplicationEvent event : events) {
            reachedByApplication.computeIfAbsent(event.getApplication().getId(), ignored -> new HashSet<>())
                    .add(event.getToStatus());
        }
        List<FunnelStageResponse> result = new ArrayList<>();
        for (ApplicationStatus status : ApplicationStatus.FUNNEL_ORDER) {
            int reached = 0;
            for (Set<ApplicationStatus> reachedSet : reachedByApplication.values()) {
                if (reachedSet.contains(status)) {
                    reached++;
                }
            }
            result.add(new FunnelStageResponse(status.name(), status.label(), reached));
        }
        return result;
    }

    /** 阶段耗时：同一投递内相邻里程碑（APPLIED 锚点用 appliedAt，后续用事件 occurredAt）之间的自然日差。 */
    private List<StageDurationResponse> buildStageDurations(
            List<JobApplication> applications,
            List<JobApplicationEvent> events
    ) {
        Map<Long, List<JobApplicationEvent>> eventsByApplication = new LinkedHashMap<>();
        for (JobApplicationEvent event : events) {
            eventsByApplication.computeIfAbsent(event.getApplication().getId(), ignored -> new ArrayList<>())
                    .add(event);
        }

        Map<String, long[]> accumulator = new LinkedHashMap<>();
        for (JobApplication application : applications) {
            List<Milestone> milestones = new ArrayList<>();
            LocalDate appliedAt = application.getAppliedAt();
            if (appliedAt != null) {
                milestones.add(new Milestone(ApplicationStatus.APPLIED, appliedAt));
            }
            for (JobApplicationEvent event : eventsByApplication.getOrDefault(application.getId(), List.of())) {
                if (event.getFromStatus() == null) {
                    continue; // 跳过创建事件，APPLIED 锚点已用 appliedAt 更精确
                }
                milestones.add(new Milestone(
                        event.getToStatus(),
                        LocalDate.ofInstant(event.getOccurredAt(), ZoneOffset.UTC)
                ));
            }
            milestones.sort(Comparator.comparing(Milestone::date));
            for (int i = 0; i + 1 < milestones.size(); i++) {
                Milestone from = milestones.get(i);
                Milestone to = milestones.get(i + 1);
                if (from.status() == to.status()) {
                    continue;
                }
                long days = ChronoUnit.DAYS.between(from.date(), to.date());
                if (days < 0) {
                    continue; // 回填的 occurredAt 早于上一里程碑，属异常数据，跳过
                }
                String key = from.status().name() + "->" + to.status().name();
                long[] bucket = accumulator.computeIfAbsent(key, ignored -> new long[2]);
                bucket[0] += days;
                bucket[1] += 1;
            }
        }

        List<Map.Entry<String, long[]>> entries = new ArrayList<>(accumulator.entrySet());
        entries.sort(Comparator
                .comparing((Map.Entry<String, long[]> entry) -> orderOf(ApplicationStatus.valueOf(fromOf(entry.getKey()))))
                .thenComparing(entry -> orderOf(ApplicationStatus.valueOf(toOf(entry.getKey())))));

        List<StageDurationResponse> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : entries) {
            ApplicationStatus from = ApplicationStatus.valueOf(fromOf(entry.getKey()));
            ApplicationStatus to = ApplicationStatus.valueOf(toOf(entry.getKey()));
            long[] bucket = entry.getValue();
            result.add(new StageDurationResponse(
                    from.name(),
                    from.label(),
                    to.name(),
                    to.label(),
                    round1((double) bucket[0] / bucket[1]),
                    (int) bucket[1]
            ));
        }
        return result;
    }

    private List<GroupStatResponse> buildGroups(List<JobApplication> applications, boolean byCompanyType) {
        Map<String, List<JobApplication>> groups = new TreeMap<>();
        for (JobApplication application : applications) {
            String raw = byCompanyType ? application.getCompanyType() : application.getCity();
            String key = raw == null || raw.isBlank() ? "未填写" : raw.trim();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(application);
        }
        List<Map.Entry<String, List<JobApplication>>> entries = new ArrayList<>(groups.entrySet());
        entries.sort(Comparator
                .comparing((Map.Entry<String, List<JobApplication>> entry) -> entry.getValue().size())
                .reversed()
                .thenComparing(Map.Entry::getKey));

        List<GroupStatResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<JobApplication>> entry : entries) {
            List<JobApplication> list = entry.getValue();
            int total = list.size();
            int offers = 0;
            int rejected = 0;
            int responded = 0;
            for (JobApplication application : list) {
                ApplicationStatus status = application.getStatus();
                if (status == ApplicationStatus.OFFER) {
                    offers++;
                } else if (status == ApplicationStatus.REJECTED) {
                    rejected++;
                }
                if (status.countsAsResponse()) {
                    responded++;
                }
            }
            double responseRate = total == 0 ? 0.0 : (double) responded / total;
            result.add(new GroupStatResponse(entry.getKey(), total, offers, rejected, responseRate));
        }
        return result;
    }

    private List<WeeklyPointResponse> buildWeekly(List<JobApplication> applications) {
        Map<LocalDate, Integer> byWeek = new TreeMap<>();
        for (JobApplication application : applications) {
            LocalDate appliedAt = application.getAppliedAt();
            if (appliedAt == null) {
                continue;
            }
            LocalDate monday = appliedAt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeek.merge(monday, 1, Integer::sum);
        }
        List<WeeklyPointResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : byWeek.entrySet()) {
            result.add(new WeeklyPointResponse(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private List<DeadlineItemResponse> buildDeadlines(List<JobApplication> applications) {
        LocalDate today = LocalDate.now();
        List<JobApplication> withDeadline = applications.stream()
                .filter(application -> application.getDeadlineAt() != null)
                .filter(application -> !application.getStatus().isTerminal())
                .sorted(Comparator.comparing(JobApplication::getDeadlineAt))
                .toList();

        List<DeadlineItemResponse> result = new ArrayList<>();
        for (JobApplication application : withDeadline) {
            long daysLeft = ChronoUnit.DAYS.between(today, application.getDeadlineAt());
            if (daysLeft > DEADLINE_WINDOW_DAYS) {
                continue;
            }
            result.add(new DeadlineItemResponse(
                    application.getId(),
                    application.getCompany(),
                    application.getPosition(),
                    application.getDeadlineAt(),
                    daysLeft,
                    daysLeft < 0
            ));
            if (result.size() >= DEADLINE_LIMIT) {
                break;
            }
        }
        return result;
    }

    private static String fromOf(String key) {
        return key.substring(0, key.indexOf("->"));
    }

    private static String toOf(String key) {
        return key.substring(key.indexOf("->") + 2);
    }

    private static int orderOf(ApplicationStatus status) {
        return status.order();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Milestone(ApplicationStatus status, LocalDate date) {
    }
}