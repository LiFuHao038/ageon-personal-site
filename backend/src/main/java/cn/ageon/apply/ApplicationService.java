package cn.ageon.apply;

import cn.ageon.apply.dto.ApplicationResponse;
import cn.ageon.apply.dto.ChangeStatusRequest;
import cn.ageon.apply.dto.CreateApplicationRequest;
import cn.ageon.apply.dto.SourcePreviewRequest;
import cn.ageon.apply.dto.SourcePreviewResponse;
import cn.ageon.apply.dto.StatusOptionResponse;
import cn.ageon.apply.dto.UpdateApplicationRequest;
import cn.ageon.auth.AuthenticatedUser;
import cn.ageon.auth.SiteUser;
import cn.ageon.common.NotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class ApplicationService {
    private final JobApplicationRepository applicationRepository;
    private final JobApplicationEventRepository eventRepository;
    /** 抓取实现未装配时为 null，新建/更新投递照常可用，只是不做链接元信息回填。 */
    private final LinkSnapshotService linkSnapshotService;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              JobApplicationEventRepository eventRepository,
                              ObjectProvider<LinkSnapshotService> linkSnapshotService) {
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.linkSnapshotService = linkSnapshotService.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(ApplicationStatus status, String keyword, String companyType,
                                          Boolean hasDeadline, Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        String keywordLower = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        String companyTypeFilter = StringUtils.hasText(companyType) ? companyType.trim() : null;
        return applicationRepository.findByOwnerId(owner.getId(), Sort.by(Sort.Direction.DESC, "appliedAt"))
                .stream()
                .filter(application -> status == null || application.getStatus() == status)
                .filter(application -> keywordLower == null || matchesKeyword(application, keywordLower))
                .filter(application -> companyTypeFilter == null
                        || companyTypeFilter.equals(application.getCompanyType()))
                .filter(application -> !Boolean.TRUE.equals(hasDeadline) || application.getDeadlineAt() != null)
                .map(ApplicationMapper::toResponse)
                .toList();
    }

    @Transactional
    public ApplicationResponse create(CreateApplicationRequest request, Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        JobApplication application = new JobApplication(
                owner, request.company().trim(), request.position().trim(), request.appliedAt());
        application.setCity(trimToNull(request.city()));
        application.setCompanyType(trimToNull(request.companyType()));
        application.setChannel(trimToNull(request.channel()));
        application.setSourceUrl(trimToNull(request.sourceUrl()));
        application.setDeadlineAt(request.deadlineAt());
        application.setNote(trimToNull(request.note()));
        ApplicationStatus initialStatus = request.appliedAt() != null
                ? ApplicationStatus.APPLIED
                : ApplicationStatus.PREPARING;
        application.setStatus(initialStatus);
        application.addEvent(new JobApplicationEvent(null, initialStatus, Instant.now(), "创建投递记录"));
        refreshSourceSnapshot(application);
        return ApplicationMapper.toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(Long id, Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        return ApplicationMapper.toResponse(findOwned(id, owner.getId()));
    }

    @Transactional
    public ApplicationResponse update(Long id, UpdateApplicationRequest request, Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        JobApplication application = findOwned(id, owner.getId());
        if (request.company() != null) {
            application.setCompany(request.company().trim());
        }
        if (request.position() != null) {
            application.setPosition(request.position().trim());
        }
        if (request.city() != null) {
            application.setCity(trimToNull(request.city()));
        }
        if (request.companyType() != null) {
            application.setCompanyType(trimToNull(request.companyType()));
        }
        if (request.channel() != null) {
            application.setChannel(trimToNull(request.channel()));
        }
        if (request.deadlineAt() != null) {
            application.setDeadlineAt(request.deadlineAt());
        }
        if (request.appliedAt() != null) {
            application.setAppliedAt(request.appliedAt());
        }
        if (request.note() != null) {
            application.setNote(trimToNull(request.note()));
        }
        if (request.sourceUrl() != null && !request.sourceUrl().equals(application.getSourceUrl())) {
            application.setSourceUrl(trimToNull(request.sourceUrl()));
            refreshSourceSnapshot(application);
        }
        return ApplicationMapper.toResponse(applicationRepository.save(application));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        applicationRepository.delete(findOwned(id, owner.getId()));
    }

    @Transactional
    public ApplicationResponse changeStatus(Long id, ChangeStatusRequest request, Authentication authentication) {
        SiteUser owner = AuthenticatedUser.requireApprovedUser(authentication);
        JobApplication application = findOwned(id, owner.getId());
        ApplicationStatus from = application.transitionTo(request.status());
        application.addEvent(new JobApplicationEvent(from, request.status(), request.occurredAt(), request.note()));
        return ApplicationMapper.toResponse(applicationRepository.save(application));
    }

    /** 状态机元信息，供前端渲染状态选项与推进按钮，无需登录态数据。 */
    public List<StatusOptionResponse> listStatusMeta() {
        return Arrays.stream(ApplicationStatus.values())
                .map(status -> new StatusOptionResponse(
                        status.name(),
                        status.label(),
                        status.isTerminal(),
                        status.allowedTransitions().stream().map(Enum::name).toList()))
                .toList();
    }

    /** 解析网申链接元信息，供表单在保存前预览并回填公司名；抓取失败不抛异常，仅回带 error。 */
    public SourcePreviewResponse previewSource(SourcePreviewRequest request) {
        String url = request.url().trim();
        if (linkSnapshotService == null) {
            return new SourcePreviewResponse(url, null, null, "链接抓取服务未启用");
        }
        LinkSnapshot snapshot = linkSnapshotService.snapshot(url);
        if (snapshot == null) {
            return new SourcePreviewResponse(url, null, null, "抓取失败");
        }
        return new SourcePreviewResponse(url, snapshot.title(), snapshot.logoUrl(), snapshot.error());
    }

    private JobApplication findOwned(Long id, Long ownerId) {
        return applicationRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("投递不存在: " + id));
    }

    /**
     * 抓取链接元信息并回填 source_* 字段。抓取消极失败（返回 error）不阻断主流程；
     * sourceUrl 为空时清空已抓取的元信息。
     */
    private void refreshSourceSnapshot(JobApplication application) {
        String sourceUrl = application.getSourceUrl();
        if (!StringUtils.hasText(sourceUrl)) {
            application.setSourceTitle(null);
            application.setSourceLogoUrl(null);
            application.setSourceError(null);
            application.setSourceFetchedAt(null);
            return;
        }
        if (linkSnapshotService == null) {
            return;
        }
        LinkSnapshot snapshot = linkSnapshotService.snapshot(sourceUrl);
        if (snapshot != null && snapshot.isSuccessful()) {
            application.setSourceTitle(snapshot.title());
            application.setSourceLogoUrl(snapshot.logoUrl());
            application.setSourceError(null);
        } else {
            application.setSourceTitle(null);
            application.setSourceLogoUrl(null);
            application.setSourceError(snapshot == null ? "抓取失败" : snapshot.error());
        }
        application.setSourceFetchedAt(Instant.now());
    }

    private static boolean matchesKeyword(JobApplication application, String keywordLower) {
        return containsIgnoreCase(application.getCompany(), keywordLower)
                || containsIgnoreCase(application.getPosition(), keywordLower)
                || containsIgnoreCase(application.getCity(), keywordLower)
                || containsIgnoreCase(application.getNote(), keywordLower);
    }

    private static boolean containsIgnoreCase(String value, String keywordLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keywordLower);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
