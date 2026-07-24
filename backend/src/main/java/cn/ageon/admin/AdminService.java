package cn.ageon.admin;

import cn.ageon.admin.dto.*;
import cn.ageon.auth.*;
import cn.ageon.auth.dto.CurrentUserResponse;
import cn.ageon.common.NotFoundException;
import cn.ageon.community.*;
import cn.ageon.community.dto.CreateReplyRequest;
import cn.ageon.community.dto.QuestionResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final SiteUserRepository userRepository;
    private final CommunityQuestionRepository questionRepository;
    private final CommunityReplyRepository replyRepository;

    public AdminService(SiteUserRepository userRepository, CommunityQuestionRepository questionRepository,
                        CommunityReplyRepository replyRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.replyRepository = replyRepository;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        long pendingUsers = userRepository.findAll().stream()
                .filter(user -> user.getStatus() == AccountStatus.PENDING).count();
        long pendingQuestions = questionRepository.findAll().stream()
                .filter(question -> question.getModerationStatus() == ModerationStatus.PENDING).count();
        long publishedQuestions = questionRepository.findAll().stream()
                .filter(question -> question.getModerationStatus() == ModerationStatus.PUBLISHED).count();
        return new AdminOverviewResponse(pendingUsers, pendingQuestions, publishedQuestions, replyRepository.count());
    }

    @Transactional(readOnly = true)
    public List<CurrentUserResponse> users(AccountStatus status) {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> status == null || user.getStatus() == status)
                .map(CurrentUserResponse::from)
                .toList();
    }

    @Transactional
    public CurrentUserResponse updateUserStatus(Long id, UpdateAccountStatusRequest request) {
        SiteUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + id));
        user.updateStatus(request.status());
        return CurrentUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> questions(ModerationStatus moderationStatus) {
        return questionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(question -> moderationStatus == null || question.getModerationStatus() == moderationStatus)
                .map(CommunityQuestionMapper::toResponse)
                .toList();
    }

    @Transactional
    public QuestionResponse moderateQuestion(Long id, UpdateModerationRequest request) {
        CommunityQuestion question = findQuestion(id);
        question.updateModerationStatus(request.moderationStatus());
        return CommunityQuestionMapper.toResponse(question);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        questionRepository.delete(findQuestion(id));
    }

    @Transactional
    public QuestionResponse reply(Long id, CreateReplyRequest request, SiteUser admin) {
        CommunityQuestion question = findQuestion(id);
        question.addReply(new CommunityReply(admin, request.content().trim()));
        return CommunityQuestionMapper.toResponse(question);
    }

    @Transactional(readOnly = true)
    public List<AdminReplyResponse> replies() {
        return replyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(reply -> new AdminReplyResponse(
                        reply.getId(), reply.getQuestion().getId(), reply.getQuestion().getTitle(),
                        reply.getAuthor(), reply.getAuthorRole(), reply.getContent(), reply.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void deleteReply(Long id) {
        CommunityReply reply = replyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("回复不存在: " + id));
        reply.getQuestion().getReplies().remove(reply);
        replyRepository.delete(reply);
    }

    private CommunityQuestion findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("问题不存在: " + id));
    }
}
