package cn.ageon.ai;

@FunctionalInterface
public interface KimiDeltaHandler {
    void onDelta(String delta);
}
