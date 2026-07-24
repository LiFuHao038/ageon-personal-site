package cn.ageon.ai;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AiDailyUsageRepository extends JpaRepository<AiDailyUsage, Long> {
    Optional<AiDailyUsage> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usage from AiDailyUsage usage " +
            "where usage.user.id = :userId and usage.usageDate = :usageDate")
    Optional<AiDailyUsage> findForUpdate(
            @Param("userId") Long userId,
            @Param("usageDate") LocalDate usageDate
    );
}
