package cn.ageon.apply;

import cn.ageon.auth.SiteUser;
import cn.ageon.common.ApiException;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_job_applications_owner",
                columnNames = {"user_id", "company", "position"}),
        indexes = {
                @Index(name = "idx_job_applications_user_status", columnList = "user_id,status"),
                @Index(name = "idx_job_applications_user_applied", columnList = "user_id,applied_at"),
                @Index(name = "idx_job_applications_user_deadline", columnList = "user_id,deadline_at")
        })
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_job_applications_user"))
    private SiteUser owner;

    @Column(nullable = false, length = 60)
    private String company;

    @Column(nullable = false, length = 80)
    private String position;

    @Column(length = 40)
    private String city;

    @Column(name = "company_type", length = 20)
    private String companyType;

    @Column(length = 20)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PREPARING;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_title", length = 200)
    private String sourceTitle;

    @Column(name = "source_logo_url", length = 500)
    private String sourceLogoUrl;

    @Column(name = "source_error", length = 200)
    private String sourceError;

    @Column(name = "source_fetched_at")
    private Instant sourceFetchedAt;

    @Column(name = "deadline_at")
    private LocalDate deadlineAt;

    @Column(name = "applied_at", nullable = false)
    private LocalDate appliedAt;

    @Column(length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC")
    private List<JobApplicationEvent> events = new ArrayList<>();

    protected JobApplication() {
    }

    public JobApplication(SiteUser owner, String company, String position, LocalDate appliedAt) {
        this.owner = owner;
        this.company = company;
        this.position = position;
        this.appliedAt = appliedAt;
        this.status = ApplicationStatus.PREPARING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * 校验并应用状态流转。事件记录由调用方负责写入，保证「状态」与「时间线」同源。
     *
     * @throws ApiException 非法流转时抛出 INVALID_STATUS_TRANSITION
     */
    public ApplicationStatus transitionTo(ApplicationStatus target) {
        ApplicationStatus from = this.status;
        if (!from.canTransitionTo(target)) {
            throw new ApiException("INVALID_STATUS_TRANSITION",
                    "不允许从「" + from.label() + "」变更为「" + target.label() + "」",
                    HttpStatus.BAD_REQUEST);
        }
        this.status = target;
        return from;
    }

    public void addEvent(JobApplicationEvent event) {
        events.add(event);
        event.setApplication(this);
    }

    public boolean isOwnedBy(Long userId) {
        return owner != null && owner.getId() != null && owner.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public SiteUser getOwner() {
        return owner;
    }

    public String getCompany() {
        return company;
    }

    public String getPosition() {
        return position;
    }

    public String getCity() {
        return city;
    }

    public String getCompanyType() {
        return companyType;
    }

    public String getChannel() {
        return channel;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public String getSourceLogoUrl() {
        return sourceLogoUrl;
    }

    public String getSourceError() {
        return sourceError;
    }

    public Instant getSourceFetchedAt() {
        return sourceFetchedAt;
    }

    public LocalDate getDeadlineAt() {
        return deadlineAt;
    }

    public LocalDate getAppliedAt() {
        return appliedAt;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<JobApplicationEvent> getEvents() {
        return events;
    }

    public void setOwner(SiteUser owner) {
        this.owner = owner;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public void setSourceLogoUrl(String sourceLogoUrl) {
        this.sourceLogoUrl = sourceLogoUrl;
    }

    public void setSourceError(String sourceError) {
        this.sourceError = sourceError;
    }

    public void setSourceFetchedAt(Instant sourceFetchedAt) {
        this.sourceFetchedAt = sourceFetchedAt;
    }

    public void setDeadlineAt(LocalDate deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public void setAppliedAt(LocalDate appliedAt) {
        this.appliedAt = appliedAt;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
