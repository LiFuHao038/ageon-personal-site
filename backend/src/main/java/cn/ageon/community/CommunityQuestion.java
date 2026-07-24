package cn.ageon.community;

import cn.ageon.auth.SiteUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_questions")
public class CommunityQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String detail;

    @Column(nullable = false, length = 40)
    private String tag;

    @Column(nullable = false, length = 40)
    private String author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private SiteUser authorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.WAITING;

    @Column(nullable = false)
    private int likes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CommunityReply> replies = new ArrayList<>();

    protected CommunityQuestion() {
    }

    public CommunityQuestion(String title, String detail, String tag, String author) {
        this.title = title;
        this.detail = detail;
        this.tag = tag;
        this.author = author;
        this.moderationStatus = ModerationStatus.PUBLISHED;
    }

    public CommunityQuestion(String title, String detail, String tag, SiteUser authorUser) {
        this.title = title;
        this.detail = detail;
        this.tag = tag;
        this.authorUser = authorUser;
        this.author = authorUser.getDisplayName();
        this.moderationStatus = ModerationStatus.PENDING;
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

    public void addReply(CommunityReply reply) {
        replies.add(reply);
        reply.setQuestion(this);
        status = QuestionStatus.ANSWERED;
    }

    public void like() {
        likes += 1;
    }

    public void updateModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getTag() {
        return tag;
    }

    public String getAuthor() {
        return author;
    }

    public SiteUser getAuthorUser() { return authorUser; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }

    public QuestionStatus getStatus() {
        return status;
    }

    public int getLikes() {
        return likes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<CommunityReply> getReplies() {
        return replies;
    }
}
