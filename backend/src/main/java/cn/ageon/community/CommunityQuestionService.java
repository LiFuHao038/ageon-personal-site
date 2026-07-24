package cn.ageon.community;

import cn.ageon.auth.AuthenticatedUser;
import cn.ageon.auth.SiteUser;
import cn.ageon.common.NotFoundException;
import cn.ageon.community.dto.CreateQuestionRequest;
import cn.ageon.community.dto.CreateReplyRequest;
import cn.ageon.community.dto.QuestionResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityQuestionService {
    private final CommunityQuestionRepository questionRepository;

    public CommunityQuestionService(CommunityQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(String keyword, String tag, QuestionStatus status) {
        return questionRepository.findAll(buildSpec(keyword, tag, status), Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(question -> question.getModerationStatus() == ModerationStatus.PUBLISHED)
                .map(CommunityQuestionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(Long id) {
        CommunityQuestion question = findQuestion(id);
        if (question.getModerationStatus() != ModerationStatus.PUBLISHED) {
            throw new NotFoundException("问题不存在: " + id);
        }
        return CommunityQuestionMapper.toResponse(question);
    }

    @Transactional
    public QuestionResponse create(CreateQuestionRequest request, Authentication authentication) {
        SiteUser author = AuthenticatedUser.requireApprovedUser(authentication);
        CommunityQuestion question = new CommunityQuestion(
                request.title().trim(),
                request.detail().trim(),
                request.tag().trim(),
                author
        );
        return CommunityQuestionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse reply(Long id, CreateReplyRequest request, Authentication authentication) {
        SiteUser author = AuthenticatedUser.requireApprovedUser(authentication);
        CommunityQuestion question = findQuestion(id);
        if (question.getModerationStatus() != ModerationStatus.PUBLISHED) {
            throw new NotFoundException("问题不存在: " + id);
        }
        question.addReply(new CommunityReply(author, request.content().trim()));
        return CommunityQuestionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse like(Long id, Authentication authentication) {
        AuthenticatedUser.requireApprovedUser(authentication);
        CommunityQuestion question = findQuestion(id);
        if (question.getModerationStatus() != ModerationStatus.PUBLISHED) {
            throw new NotFoundException("问题不存在: " + id);
        }
        question.like();
        return CommunityQuestionMapper.toResponse(questionRepository.save(question));
    }

    public CommunityQuestion findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("问题不存在: " + id));
    }

    private Specification<CommunityQuestion> buildSpec(String keyword, String tag, QuestionStatus status) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), like),
                        builder.like(builder.lower(root.get("detail")), like),
                        builder.like(builder.lower(root.get("tag")), like)
                ));
            }
            if (StringUtils.hasText(tag) && !"全部".equals(tag.trim())) {
                predicates.add(builder.equal(root.get("tag"), tag.trim()));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
