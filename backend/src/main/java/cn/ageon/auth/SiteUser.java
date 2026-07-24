package cn.ageon.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "site_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_site_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_site_users_email", columnNames = "email")
})
public class SiteUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String displayName;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(name = "ai_daily_limit", nullable = false)
    private int aiDailyLimit = 20;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SiteUser() {
    }

    private SiteUser(String displayName, String username, String email, String passwordHash,
                     UserRole role, AccountStatus status) {
        this.displayName = displayName.trim();
        this.username = normalize(username);
        this.email = normalize(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public static SiteUser pending(String displayName, String username, String email, String passwordHash) {
        return new SiteUser(displayName, username, email, passwordHash, UserRole.USER, AccountStatus.PENDING);
    }

    public static SiteUser administrator(String username, String email, String passwordHash) {
        return new SiteUser("AGEON Admin", username, email, passwordHash, UserRole.ADMIN, AccountStatus.APPROVED);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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

    public void updateStatus(AccountStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public int getAiDailyLimit() { return aiDailyLimit; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
