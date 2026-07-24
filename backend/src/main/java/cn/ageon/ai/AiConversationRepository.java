package cn.ageon.ai;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    @EntityGraph(attributePaths = "messages")
    List<AiConversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "messages")
    @Query("select conversation from AiConversation conversation " +
            "where conversation.id = :id and conversation.user.id = :userId")
    Optional<AiConversation> findOwnedWithMessages(@Param("id") Long id, @Param("userId") Long userId);

    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
}
