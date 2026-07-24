package cn.ageon.ai;

import java.util.List;

public interface AiModelClient {
    void assertConfigured();

    void stream(String model, List<KimiChatMessage> messages, KimiDeltaHandler deltaHandler);
}
