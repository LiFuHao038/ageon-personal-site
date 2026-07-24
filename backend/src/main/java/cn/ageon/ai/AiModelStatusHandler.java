package cn.ageon.ai;

@FunctionalInterface
public interface AiModelStatusHandler {
    void onFallback(String model);
}
