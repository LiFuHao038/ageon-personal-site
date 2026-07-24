package cn.ageon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    @ConfigurationProperties(prefix = "ageon.cors")
    CorsProperties corsProperties() {
        return new CorsProperties();
    }

    @Bean
    CorsFilter corsFilter(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return new CorsFilter(source);
    }

    public static class CorsProperties {
        private static final String ENVIRONMENT_PREFIX = "AGEON_CORS_ALLOWED_ORIGINS=";

        private List<String> allowedOrigins = List.of(
                "http://localhost:3000",
                "https://gbohmvqgmafa.cloud.sealos.io"
        );

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins.stream()
                    .map(String::trim)
                    .map(CorsProperties::removeEnvironmentPrefix)
                    .map(CorsProperties::removeTrailingSlash)
                    .filter(origin -> !origin.isBlank())
                    .distinct()
                    .toList();
        }

        private static String removeEnvironmentPrefix(String origin) {
            return origin.startsWith(ENVIRONMENT_PREFIX)
                    ? origin.substring(ENVIRONMENT_PREFIX.length())
                    : origin;
        }

        private static String removeTrailingSlash(String origin) {
            return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        }
    }
}
