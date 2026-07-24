package cn.ageon.ai;

import cn.ageon.ai.dto.AiConversationDetailResponse;
import cn.ageon.ai.dto.AiConversationSummaryResponse;
import cn.ageon.auth.AuthenticatedUser;
import cn.ageon.auth.SiteUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/conversations")
public class AiConversationController {
    private final AiConversationService conversationService;
    private final AiStreamService streamService;

    public AiConversationController(AiConversationService conversationService, AiStreamService streamService) {
        this.conversationService = conversationService;
        this.streamService = streamService;
    }

    @GetMapping
    public List<AiConversationSummaryResponse> list(Authentication authentication) {
        return conversationService.list(approvedUser(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiConversationSummaryResponse create(Authentication authentication) {
        return conversationService.create(approvedUser(authentication));
    }

    @GetMapping("/{id}")
    public AiConversationDetailResponse get(@PathVariable Long id, Authentication authentication) {
        return conversationService.get(id, approvedUser(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        conversationService.delete(id, approvedUser(authentication));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(
            @PathVariable Long id,
            @RequestBody cn.ageon.ai.dto.StreamMessageRequest request,
            Authentication authentication
    ) {
        return streamService.start(id, approvedUser(authentication), request.content());
    }

    private static SiteUser approvedUser(Authentication authentication) {
        return AuthenticatedUser.requireApprovedUser(authentication);
    }
}
