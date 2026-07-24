package cn.ageon.ai;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "ai_message", indexes = {
        @Index(name = "idx_ai_message_conversation_created", columnList = "conversation_id, created_at")
})
public class AiMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiMessage() {
    }

    private AiMessage(AiMessageRole role, AiMessageStatus status, String content) {
        this.role = role;
        this.status = status;
        this.content = content.trim();
    }

    public static AiMessage user(String content) {
        return new AiMessage(AiMessageRole.USER, AiMessageStatus.COMPLETED, content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage(AiMessageRole.ASSISTANT, AiMessageStatus.COMPLETED, content);
    }

    public static AiMessage failed(String message) {
        return new AiMessage(AiMessageRole.ASSISTANT, AiMessageStatus.FAILED, message);
    }

    void attachTo(AiConversation conversation) {
        this.conversation = conversation;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AiConversation getConversation() { return conversation; }
    public AiMessageRole getRole() { return role; }
    public AiMessageStatus getStatus() { return status; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
