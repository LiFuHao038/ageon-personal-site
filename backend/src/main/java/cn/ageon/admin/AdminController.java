package cn.ageon.admin;

import cn.ageon.admin.dto.*;
import cn.ageon.auth.*;
import cn.ageon.auth.dto.CurrentUserResponse;
import cn.ageon.community.ModerationStatus;
import cn.ageon.community.dto.CreateReplyRequest;
import cn.ageon.community.dto.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() { return adminService.overview(); }

    @GetMapping("/users")
    public List<CurrentUserResponse> users(@RequestParam(required = false) AccountStatus status) {
        return adminService.users(status);
    }

    @PatchMapping("/users/{id}/status")
    public CurrentUserResponse updateUserStatus(@PathVariable Long id,
                                                @Valid @RequestBody UpdateAccountStatusRequest request) {
        return adminService.updateUserStatus(id, request);
    }

    @GetMapping("/questions")
    public List<QuestionResponse> questions(@RequestParam(required = false) ModerationStatus moderationStatus) {
        return adminService.questions(moderationStatus);
    }

    @PatchMapping("/questions/{id}/moderation")
    public QuestionResponse moderateQuestion(@PathVariable Long id,
                                             @Valid @RequestBody UpdateModerationRequest request) {
        return adminService.moderateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long id) { adminService.deleteQuestion(id); }

    @PostMapping("/questions/{id}/replies")
    public QuestionResponse reply(@PathVariable Long id, @Valid @RequestBody CreateReplyRequest request,
                                  Authentication authentication) {
        SiteUser admin = AuthenticatedUser.requireApprovedUser(authentication);
        return adminService.reply(id, request, admin);
    }

    @GetMapping("/replies")
    public List<AdminReplyResponse> replies() { return adminService.replies(); }

    @DeleteMapping("/replies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(@PathVariable Long id) { adminService.deleteReply(id); }
}
