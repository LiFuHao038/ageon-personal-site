package cn.ageon.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ageon.ai.primary-model=qwen-plus",
        "ageon.ai.fallback-enabled=true",
        "ageon.ai.fallback-model=kimi/kimi-k3"
})
@ActiveProfiles("test")
class AiPropertiesTest {
    @Autowired
    AiProperties properties;

    @Test
    void bindsPrimaryAndFallbackModels() {
        assertThat(properties.getPrimaryModel()).isEqualTo("qwen-plus");
        assertThat(properties.getFallbackModel()).isEqualTo("kimi/kimi-k3");
        assertThat(properties.isFallbackEnabled()).isTrue();
    }
}
