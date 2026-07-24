package cn.ageon.ai;

public record KimiStreamChunk(String delta, boolean done) {
    public static KimiStreamChunk delta(String value) {
        return new KimiStreamChunk(value, false);
    }

    public static KimiStreamChunk doneChunk() {
        return new KimiStreamChunk("", true);
    }
}
