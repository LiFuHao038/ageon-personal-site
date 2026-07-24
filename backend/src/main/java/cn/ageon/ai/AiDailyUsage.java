package cn.ageon.ai;

import cn.ageon.auth.SiteUser;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ai_daily_usage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_daily_usage_user_date", columnNames = {"user_id", "usage_date"})
})
public class AiDailyUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteUser user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiDailyUsage() {
    }

    private AiDailyUsage(SiteUser user, LocalDate usageDate) {
        this.user = user;
        this.usageDate = usageDate;
    }

    public static AiDailyUsage create(SiteUser user, LocalDate usageDate) {
        return new AiDailyUsage(user, usageDate);
    }

    public void reserve(int dailyLimit) {
        if (usedCount >= dailyLimit) {
            throw new IllegalStateException("AI daily limit reached");
        }
        usedCount += 1;
    }

    public void release() {
        if (usedCount > 0) {
            usedCount -= 1;
        }
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public SiteUser getUser() { return user; }
    public LocalDate getUsageDate() { return usageDate; }
    public int getUsedCount() { return usedCount; }
    public Instant getUpdatedAt() { return updatedAt; }
}
