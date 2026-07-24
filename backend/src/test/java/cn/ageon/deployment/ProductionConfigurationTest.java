package cn.ageon.deployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionConfigurationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void exposesUnauthenticatedHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void flywayBaselineContainsAllApplicationTablesAndCascades() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__baseline.sql");
        assertThat(migration.exists()).isTrue();
        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains(
                "CREATE TABLE `site_users`",
                "CREATE TABLE `community_questions`",
                "CREATE TABLE `community_replies`",
                "CREATE TABLE `ai_conversation`",
                "CREATE TABLE `ai_message`",
                "CREATE TABLE `ai_daily_usage`",
                "`ai_daily_limit` INT NOT NULL DEFAULT 20",
                "UNIQUE KEY `uk_ai_daily_usage_user_date`",
                "ON DELETE CASCADE"
        );
    }
}
