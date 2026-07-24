package cn.ageon.common;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, details, Instant.now());
    }
}
