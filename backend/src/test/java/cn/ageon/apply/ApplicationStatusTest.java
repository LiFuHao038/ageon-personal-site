package cn.ageon.apply;

import cn.ageon.auth.SiteUser;
import cn.ageon.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStatusTest {

    @Test
    void appliedCanJumpForwardToInterview1() {
        // 允许沿漏斗向前跳跃（跳过笔试直接面试）
        assertTrue(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.INTERVIEW_1));
    }

    @Test
    void appliedCannotMoveBackToPreparing() {
        // 不允许后退到漏斗序更小的状态
        assertFalse(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.PREPARING));
    }

    @Test
    void offerCanBeWithdrawn() {
        // 终态唯一例外：接了别家，OFFER → WITHDRAWN 合法
        assertTrue(ApplicationStatus.OFFER.canTransitionTo(ApplicationStatus.WITHDRAWN));
    }

    @Test
    void rejectedIsTerminal() {
        for (ApplicationStatus target : ApplicationStatus.values()) {
            assertFalse(ApplicationStatus.REJECTED.canTransitionTo(target),
                    "REJECTED 是终态，不应允许流转到 " + target);
        }
    }

    @Test
    void sameStatusIsNotAllowed() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertFalse(status.canTransitionTo(status), "同状态重复设置应非法: " + status);
        }
    }

    @Test
    void interviewFinalCanReachOffer() {
        assertTrue(ApplicationStatus.INTERVIEW_FINAL.canTransitionTo(ApplicationStatus.OFFER));
    }

    @Test
    void transitionToReturnsPreviousStatus() {
        JobApplication application = newApplication();
        ApplicationStatus from = application.transitionTo(ApplicationStatus.WRITTEN_TEST);
        assertEquals(ApplicationStatus.APPLIED, from);
        assertEquals(ApplicationStatus.WRITTEN_TEST, application.getStatus());
    }

    @Test
    void transitionToRejectsIllegalTransition() {
        JobApplication application = newApplication();
        ApiException exception = assertThrows(ApiException.class,
                () -> application.transitionTo(ApplicationStatus.PREPARING));
        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        assertEquals(ApplicationStatus.APPLIED, application.getStatus());
    }

    private static JobApplication newApplication() {
        JobApplication application = new JobApplication(
                SiteUser.pending("状态机用户", "status-machine-user", "status-machine@example.com", "hash"),
                "测试公司", "后端开发", LocalDate.of(2026, 7, 20));
        application.setStatus(ApplicationStatus.APPLIED);
        return application;
    }
}
