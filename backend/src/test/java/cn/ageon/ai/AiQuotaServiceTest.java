package cn.ageon.ai;

import cn.ageon.auth.AccountStatus;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
import cn.ageon.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AiQuotaServiceTest {
    @Autowired AiQuotaService quotaService;
    @Autowired AiDailyUsageRepository usageRepository;
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private SiteUser user;

    @BeforeEach
    void setUp() {
        usageRepository.deleteAll();
        user = userRepository.findByUsernameIgnoreCase("ai-quota-user")
                .orElseGet(() -> SiteUser.pending(
                        "额度测试用户", "ai-quota-user", "ai-quota@example.com",
                        passwordEncoder.encode("Password123!")
                ));
        user.updateStatus(AccountStatus.APPROVED);
        user = userRepository.saveAndFlush(user);
    }

    @Test
    void reservesUpToDailyLimitAndRejectsNextRequest() {
        AiQuotaReservation lastReservation = null;
        for (int request = 0; request < 20; request += 1) {
            lastReservation = quotaService.reserve(user.getId());
        }

        assertThat(lastReservation).isNotNull();
        assertThat(quotaService.current(user.getId()).used()).isEqualTo(20);
        assertThat(quotaService.current(user.getId()).remaining()).isZero();
        assertThatThrownBy(() -> quotaService.reserve(user.getId()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("AI_DAILY_LIMIT_REACHED");
                    assertThat(exception.getStatus().value()).isEqualTo(429);
                });
    }

    @Test
    void releasesReservationAfterFailedUpstreamCall() {
        AiQuotaReservation reservation = quotaService.reserve(user.getId());
        assertThat(quotaService.current(user.getId()).used()).isEqualTo(1);

        quotaService.release(reservation);

        assertThat(quotaService.current(user.getId()).used()).isZero();
        assertThat(quotaService.current(user.getId()).remaining()).isEqualTo(20);
    }

    @Test
    void concurrentReservationsNeverExceedDailyLimit() throws Exception {
        int attempts = 30;
        var executor = Executors.newFixedThreadPool(10);
        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int index = 0; index < attempts; index += 1) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                try {
                    quotaService.reserve(user.getId());
                    return true;
                } catch (ApiException exception) {
                    if (!"AI_DAILY_LIMIT_REACHED".equals(exception.getCode())) throw exception;
                    return false;
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        int accepted = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(20, TimeUnit.SECONDS)) accepted += 1;
        }
        executor.shutdownNow();

        assertThat(accepted).isEqualTo(20);
        assertThat(quotaService.current(user.getId()).used()).isEqualTo(20);
        assertThat(usageRepository.count()).isEqualTo(1);
    }
}
