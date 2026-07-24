package cn.ageon.ai;

import cn.ageon.auth.AccountStatus;
import cn.ageon.auth.SiteUser;
import cn.ageon.auth.SiteUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiPersistenceModelTest {
    @Autowired SiteUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void persistsConversationMessagesUsageAndDefaultLimit() {
        SiteUser user = SiteUser.pending(
                "AI 测试用户", "ai-model-user", "ai-model@example.com",
                passwordEncoder.encode("Password123!")
        );
        user.updateStatus(AccountStatus.APPROVED);
        user = userRepository.saveAndFlush(user);

        AiConversation conversation = AiConversation.create(user);
        conversation.renameFromFirstMessage("TCP 为什么需要三次握手？");
        conversation.addMessage(AiMessage.user("TCP 为什么需要三次握手？"));
        conversation.addMessage(AiMessage.assistant("三次握手用于确认双方收发能力和初始序列号。"));
        entityManager.persist(conversation);

        AiDailyUsage usage = AiDailyUsage.create(user, LocalDate.of(2026, 7, 23));
        usage.reserve(user.getAiDailyLimit());
        entityManager.persist(usage);
        entityManager.flush();
        entityManager.clear();

        AiConversation stored = entityManager.find(AiConversation.class, conversation.getId());
        AiDailyUsage storedUsage = entityManager.find(AiDailyUsage.class, usage.getId());
        SiteUser storedUser = entityManager.find(SiteUser.class, user.getId());

        assertThat(stored.getTitle()).isEqualTo("TCP 为什么需要三次握手？");
        assertThat(stored.getMessages()).hasSize(2);
        assertThat(stored.getMessages().get(0).getRole()).isEqualTo(AiMessageRole.USER);
        assertThat(stored.getMessages().get(1).getStatus()).isEqualTo(AiMessageStatus.COMPLETED);
        assertThat(storedUsage.getUsageDate()).isEqualTo(LocalDate.of(2026, 7, 23));
        assertThat(storedUsage.getUsedCount()).isEqualTo(1);
        assertThat(storedUser.getAiDailyLimit()).isEqualTo(20);
    }

    @Test
    void physicallyDeletesMessagesWithConversation() {
        SiteUser user = SiteUser.pending(
                "级联测试用户", "ai-cascade-user", "ai-cascade@example.com",
                passwordEncoder.encode("Password123!")
        );
        user = userRepository.saveAndFlush(user);
        AiConversation conversation = AiConversation.create(user);
        conversation.addMessage(AiMessage.user("待删除消息"));
        entityManager.persist(conversation);
        entityManager.flush();
        Long messageId = conversation.getMessages().get(0).getId();

        entityManager.remove(conversation);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(AiMessage.class, messageId)).isNull();
    }

    @Test
    void mysqlSchemaContainsAiTablesQuotaAndCascadeDelete() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/mysql/ai-schema.sql");
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("ALTER TABLE `site_users`");
        assertThat(sql).contains("`ai_daily_limit` INT NOT NULL DEFAULT 20");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ai_conversation`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ai_message`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ai_daily_usage`");
        assertThat(sql).contains("UNIQUE KEY `uk_ai_daily_usage_user_date`");
        assertThat(sql).contains("ON DELETE CASCADE");
    }
}
