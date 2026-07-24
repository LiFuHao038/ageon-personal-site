package cn.ageon.ai;

import cn.ageon.ai.dto.AiQuotaResponse;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
import cn.ageon.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class AiQuotaService {
    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Shanghai");

    private final AiDailyUsageRepository usageRepository;
    private final SiteUserRepository userRepository;

    public AiQuotaService(AiDailyUsageRepository usageRepository, SiteUserRepository userRepository) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AiQuotaResponse current(Long userId) {
        SiteUser user = requireUser(userId);
        LocalDate usageDate = currentDate();
        int used = usageRepository.findByUserIdAndUsageDate(userId, usageDate)
                .map(AiDailyUsage::getUsedCount)
                .orElse(0);
        return response(usageDate, user.getAiDailyLimit(), used);
    }

    @Transactional
    public AiQuotaReservation reserve(Long userId) {
        SiteUser user = lockUser(userId);
        LocalDate usageDate = currentDate();
        AiDailyUsage usage = usageRepository.findForUpdate(userId, usageDate)
                .orElseGet(() -> usageRepository.saveAndFlush(AiDailyUsage.create(user, usageDate)));

        if (usage.getUsedCount() >= user.getAiDailyLimit()) {
            throw new ApiException(
                    "AI_DAILY_LIMIT_REACHED",
                    "今日 AI 问答额度已用完，请明天再试",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
        usage.reserve(user.getAiDailyLimit());
        usageRepository.save(usage);
        return new AiQuotaReservation(userId, usageDate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(AiQuotaReservation reservation) {
        lockUser(reservation.userId());
        usageRepository.findForUpdate(reservation.userId(), reservation.usageDate())
                .ifPresent(AiDailyUsage::release);
    }

    private SiteUser lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND));
    }

    private SiteUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND));
    }

    private LocalDate currentDate() {
        return LocalDate.now(QUOTA_ZONE);
    }

    private AiQuotaResponse response(LocalDate date, int dailyLimit, int used) {
        return new AiQuotaResponse(
                date,
                dailyLimit,
                used,
                Math.max(0, dailyLimit - used),
                date.plusDays(1).atStartOfDay(QUOTA_ZONE).toOffsetDateTime()
        );
    }
}
