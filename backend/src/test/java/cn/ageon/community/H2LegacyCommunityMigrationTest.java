package cn.ageon.community;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class H2LegacyCommunityMigrationTest {
    @Test
    void fillsRequiredCommunityColumnsForLegacyRows() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:legacy-community;MODE=MySQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("create table community_questions (id bigint primary key, title varchar(100))");
                statement.execute("insert into community_questions (id, title) values (1, 'legacy question')");
                statement.execute("create table community_replies (id bigint primary key, author varchar(40))");
                statement.execute("insert into community_replies (id, author) values (1, 'legacy user')");

                String migration = new String(
                        H2LegacyCommunityMigrationTest.class.getResourceAsStream(
                                "/db/h2/dev-community-migration.sql"
                        ).readAllBytes(),
                        StandardCharsets.UTF_8
                );
                for (String sql : migration.split(";")) {
                    if (!sql.isBlank()) statement.execute(sql.trim());
                }

                try (var result = statement.executeQuery(
                        "select moderation_status from community_questions where id = 1"
                )) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("PUBLISHED");
                }
                try (var result = statement.executeQuery(
                        "select author_role from community_replies where id = 1"
                )) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("USER");
                }
            }
        }
    }
}
