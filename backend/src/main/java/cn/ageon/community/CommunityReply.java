package cn.ageon.community;

import cn.ageon.auth.SiteUser;
import cn.ageon.auth.UserRole;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "community_replies")
public class CommunityReply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private CommunityQuestion question;

    @Column(nullable = false, length = 40)
    private String author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private SiteUser authorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole authorRole = UserRole.USER;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected CommunityReply() {
    }

    public CommunityReply(String author, String content) {
        this.author = author;
        this.content = content;
    }

    public CommunityReply(SiteUser authorUser, String content) {
        this.authorUser = authorUser;
        this.author = authorUser.getDisplayName();
        this.authorRole = authorUser.getRole();
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    void setQuestion(CommunityQuestion question) {
        this.question = question;
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UserRole getAuthorRole() { return authorRole; }
    public CommunityQuestion getQuestion() { return question; }
}
