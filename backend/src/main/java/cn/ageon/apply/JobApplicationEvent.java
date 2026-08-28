package cn.ageon.apply;

import cn.ageon.common.ApiException;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * 投递状态流转事件，是时间线与统计的唯一真相来源。
 *
 * <p>只存当前状态就算不出「投递后隔几天收到笔试」，因此每次流转都追加一条不可变事件。
 * {@code occurredAt} 是业务发生时间（允许用户回填），不是入库时间。
 */
@Entity
@Table(name = "job_application_events",
        indexes = @Index(name = "idx_job_application_events_app_time", columnList = "application_id,occurred_at"))
public class JobApplicationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", foreignKey = @ForeignKey(name = "fk_job_application_events_application"))
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(length = 300)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JobApplicationEvent() {
    }

    public JobApplicationEvent(ApplicationStatus fromStatus, ApplicationStatus toStatus,
                               Instant occurredAt, String note) {
        if (toStatus == null) {
            throw new ApiException("INVALID_STATUS", "目标状态不能为空", HttpStatus.BAD_REQUEST);
        }
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public JobApplication getApplication() {
        return application;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void setApplication(JobApplication application) {
        this.application = application;
    }
}
