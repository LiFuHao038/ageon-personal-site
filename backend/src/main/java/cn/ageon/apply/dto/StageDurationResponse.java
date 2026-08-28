package cn.ageon.apply.dto;

public record StageDurationResponse(
        String from,
        String fromLabel,
        String to,
        String toLabel,
        double averageDays,
        int samples
) {
}