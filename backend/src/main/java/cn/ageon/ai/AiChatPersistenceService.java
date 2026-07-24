package cn.ageon.ai;

import cn.ageon.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiChatPersistenceService {
    private final AiConversationRepository conversationRepository;

    public AiChatPersistenceService(AiConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional(readOnly = true)
    public List<KimiChatMessage> loadHistory(Long conversationId, Long userId) {
        return owned(conversationId, userId).getMessages().stream()
                .filter(message -> message.getStatus() == AiMessageStatus.COMPLETED)
                .map(message -> new KimiChatMessage(
                        message.getRole() == AiMessageRole.USER ? "user" : "assistant",
                        message.getContent()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getTitle(Long conversationId, Long userId) {
        return owned(conversationId, userId).getTitle();
    }

    @Transactional
    public void saveUserMessage(Long conversationId, Long userId, String content) {
        AiConversation conversation = owned(conversationId, userId);
        boolean firstUserMessage = conversation.getMessages().stream()
                .noneMatch(message -> message.getRole() == AiMessageRole.USER);
        if (firstUserMessage) conversation.renameFromFirstMessage(content);
        conversation.addMessage(AiMessage.user(content));
        conversationRepository.save(conversation);
    }

    @Transactional
    public AiMessage saveAssistantMessage(Long conversationId, Long userId, String content) {
        AiConversation conversation = owned(conversationId, userId);
        AiMessage message = AiMessage.assistant(content);
        conversation.addMessage(message);
        conversationRepository.saveAndFlush(conversation);
        return message;
    }

    @Transactional
    public void saveFailedAttempt(Long conversationId, Long userId, Long completedMessageId, String message) {
        AiConversation conversation = owned(conversationId, userId);
        if (completedMessageId != null) conversation.removeMessage(completedMessageId);
        conversation.addMessage(AiMessage.failed(message));
        conversationRepository.save(conversation);
    }

    private AiConversation owned(Long conversationId, Long userId) {
        return conversationRepository.findOwnedWithMessages(conversationId, userId)
                .orElseThrow(() -> new ApiException(
                        "AI_CONVERSATION_NOT_FOUND", "AI 会话不存在", HttpStatus.NOT_FOUND
                ));
    }
}
