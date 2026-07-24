package cn.ageon.ai;

import cn.ageon.auth.SiteUser;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversation", indexes = {
        @Index(name = "idx_ai_conversation_user_updated", columnList = "user_id, updated_at")
})
public class AiConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteUser user;

    @Column(nullable = false, length = 80)
    private String title = "新对话";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<AiMessage> messages = new ArrayList<>();

    protected AiConversation() {
    }

    private AiConversation(SiteUser user) {
        this.user = user;
    }

    public static AiConversation create(SiteUser user) {
        return new AiConversation(user);
    }

    public void renameFromFirstMessage(String content) {
        String normalized = content.trim().replaceAll("\\s+", " ");
        title = normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    public void addMessage(AiMessage message) {
        messages.add(message);
        message.attachTo(this);
        updatedAt = Instant.now();
    }

    public void removeMessage(Long messageId) {
        messages.removeIf(message -> message.getId().equals(messageId));
        updatedAt = Instant.now();
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

    public Long getId() { return id; }
    public SiteUser getUser() { return user; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AiMessage> getMessages() { return messages; }
}
