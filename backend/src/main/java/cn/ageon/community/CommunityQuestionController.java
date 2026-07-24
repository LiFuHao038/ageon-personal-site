package cn.ageon.community;

import cn.ageon.community.dto.CreateQuestionRequest;
import cn.ageon.community.dto.CreateReplyRequest;
import cn.ageon.community.dto.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community")
public class CommunityQuestionController {
    private final CommunityQuestionService questionService;

    public CommunityQuestionController(CommunityQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/questions")
    public List<QuestionResponse> listQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) QuestionStatus status
    ) {
        return questionService.list(keyword, tag, status);
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(@Valid @RequestBody CreateQuestionRequest request,
                                           Authentication authentication) {
        return questionService.create(request, authentication);
    }

    @GetMapping("/questions/{id}")
    public QuestionResponse getQuestion(@PathVariable Long id) {
        return questionService.get(id);
    }

    @PostMapping("/questions/{id}/replies")
    public QuestionResponse createReply(@PathVariable Long id, @Valid @RequestBody CreateReplyRequest request,
                                        Authentication authentication) {
        return questionService.reply(id, request, authentication);
    }

    @PostMapping("/questions/{id}/likes")
    public QuestionResponse likeQuestion(@PathVariable Long id, Authentication authentication) {
        return questionService.like(id, authentication);
    }

    @GetMapping("/tags")
    public List<String> listTags() {
        return List.of("全部", "Java 后端", "AI 应用", "计算机网络", "数据库");
    }
}
