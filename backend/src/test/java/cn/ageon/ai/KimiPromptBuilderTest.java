package cn.ageon.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KimiPromptBuilderTest {
    @Test
    void dropsOldestHistoryUntilPromptFitsTokenBudget() {
        KimiPromptBuilder builder = new KimiPromptBuilder(180, 40);
        List<KimiChatMessage> history = List.of(
                new KimiChatMessage("user", "旧".repeat(70)),
                new KimiChatMessage("assistant", "中".repeat(35)),
                new KimiChatMessage("user", "新".repeat(20))
        );

        List<KimiChatMessage> prompt = builder.build(history, "当前问题");

        assertThat(prompt).extracting(KimiChatMessage::content)
                .doesNotContain("旧".repeat(70))
                .contains("中".repeat(35), "新".repeat(20), "当前问题");
    }

    @Test
    void rejectsCurrentMessageThatCannotFitWithoutHistory() {
        KimiPromptBuilder builder = new KimiPromptBuilder(100, 40);

        assertThatThrownBy(() -> builder.build(List.of(), "超".repeat(100)))
                .isInstanceOfSatisfying(cn.ageon.common.ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AI_MESSAGE_TOO_LARGE"));
    }
}
