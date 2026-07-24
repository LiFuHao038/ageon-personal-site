package cn.ageon.ai;

import cn.ageon.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class KimiPromptBuilder {
    static final String SYSTEM_PROMPT = """
            你是 AGEON，一个简洁、准确的中文技术助手。优先解释原理，并在需要时给出可执行示例。
            AGEON 不是独立训练的基础模型：默认由 qwen-plus 生成回答；主模型限流、不可用或首个 Token 前超时时，系统可能切换到 kimi/kimi-k3。
            当用户询问模型身份时，请如实说明这套主备路由，不要编造自研模型、推理代理、训练参数或无法确认的内部架构。
            """;
    private static final int CHARACTER_LIMIT = 24_000;
    private static final int MESSAGE_OVERHEAD = 4;

    private final int contextWindowTokens;
    private final int maxOutputTokens;

    public KimiPromptBuilder(int contextWindowTokens, int maxOutputTokens) {
        this.contextWindowTokens = contextWindowTokens;
        this.maxOutputTokens = maxOutputTokens;
    }

    public List<KimiChatMessage> build(List<KimiChatMessage> history, String currentMessage) {
        int budget = (int) Math.floor((contextWindowTokens - maxOutputTokens) * 0.9);
        KimiChatMessage system = new KimiChatMessage("system", SYSTEM_PROMPT);
        KimiChatMessage current = new KimiChatMessage("user", currentMessage);
        if (budget <= 0 || estimatedTokens(List.of(system, current)) > budget) {
            throw new ApiException("AI_MESSAGE_TOO_LARGE", "消息超出当前模型的上下文限制", HttpStatus.BAD_REQUEST);
        }

        List<KimiChatMessage> selected = new ArrayList<>(history.size());
        selected.addAll(history.subList(Math.max(0, history.size() - 20), history.size()));
        while (!selected.isEmpty() && (estimatedTokens(withRequired(system, selected, current)) > budget
                || characterCount(selected, current) > CHARACTER_LIMIT)) {
            selected.removeFirst();
        }
        return withRequired(system, selected, current);
    }

    private static List<KimiChatMessage> withRequired(
            KimiChatMessage system, List<KimiChatMessage> history, KimiChatMessage current
    ) {
        List<KimiChatMessage> messages = new ArrayList<>(history.size() + 2);
        messages.add(system);
        messages.addAll(history);
        messages.add(current);
        return List.copyOf(messages);
    }

    private static int characterCount(List<KimiChatMessage> history, KimiChatMessage current) {
        return history.stream().mapToInt(message -> message.content().length()).sum() + current.content().length();
    }

    private static int estimatedTokens(List<KimiChatMessage> messages) {
        return messages.stream().mapToInt(message -> estimate(message.content()) + MESSAGE_OVERHEAD).sum();
    }

    private static int estimate(String value) {
        int tokens = 0;
        int asciiRun = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint < 128 && (Character.isLetterOrDigit(codePoint) || Character.isWhitespace(codePoint))) {
                asciiRun += 1;
                continue;
            }
            tokens += (asciiRun + 3) / 4;
            asciiRun = 0;
            tokens += 1;
        }
        return tokens + (asciiRun + 3) / 4;
    }
}
