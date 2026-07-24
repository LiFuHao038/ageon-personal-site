package cn.ageon.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {
    @Test
    void allowsDeployedFrontendByDefault() {
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();

        assertThat(properties.getAllowedOrigins())
                .contains("https://gbohmvqgmafa.cloud.sealos.io");
    }

    @Test
    void normalizesSealosEnvironmentVariableValue() {
        CorsConfig.CorsProperties properties = new CorsConfig.CorsProperties();

        properties.setAllowedOrigins(List.of(
                " AGEON_CORS_ALLOWED_ORIGINS=https://gbohmvqgmafa.cloud.sealos.io/ "
        ));

        assertThat(properties.getAllowedOrigins())
                .containsExactly("https://gbohmvqgmafa.cloud.sealos.io");
    }
}
