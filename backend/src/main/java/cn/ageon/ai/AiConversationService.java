package cn.ageon.ai;

import cn.ageon.ai.dto.AiConversationDetailResponse;
import cn.ageon.ai.dto.AiConversationSummaryResponse;
import cn.ageon.auth.SiteUser;
import cn.ageon.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiConversationService {
    private final AiConversationRepository conversationRepository;

    public AiConversationService(AiConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional(readOnly = true)
    public List<AiConversationSummaryResponse> list(SiteUser user) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(AiConversationSummaryResponse::from)
                .toList();
    }

    @Transactional
    public AiConversationSummaryResponse create(SiteUser user) {
        return AiConversationSummaryResponse.from(
                conversationRepository.save(AiConversation.create(user))
        );
    }

    @Transactional(readOnly = true)
    public AiConversationDetailResponse get(Long id, SiteUser user) {
        return AiConversationDetailResponse.from(findOwnedWithMessages(id, user.getId()));
    }

    @Transactional
    public void delete(Long id, SiteUser user) {
        AiConversation conversation = conversationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(AiConversationService::notFound);
        conversationRepository.delete(conversation);
    }

    private AiConversation findOwnedWithMessages(Long id, Long userId) {
        return conversationRepository.findOwnedWithMessages(id, userId)
                .orElseThrow(AiConversationService::notFound);
    }

    private static ApiException notFound() {
        return new ApiException(
                "AI_CONVERSATION_NOT_FOUND",
                "AI 会话不存在",
                HttpStatus.NOT_FOUND
        );
    }
}
