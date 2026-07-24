package cn.ageon.ai;

public class AiModelException extends RuntimeException {
    private final String code;
    private final boolean fallbackEligible;
    private final Integer httpStatus;
    private final String requestId;

    public AiModelException(
            String code,
            String message,
            boolean fallbackEligible,
            Integer httpStatus,
            String requestId,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.fallbackEligible = fallbackEligible;
        this.httpStatus = httpStatus;
        this.requestId = requestId == null ? "" : requestId;
    }

    public static AiModelException timeout(Throwable cause) {
        return new AiModelException(
                "AI_MODEL_BUSY", "模型服务繁忙，请稍后再试",
                true, null, "", cause
        );
    }

    public static AiModelException connection(Throwable cause) {
        return new AiModelException(
                "AI_MODEL_UNAVAILABLE", "模型服务暂时不可用，请稍后再试",
                false, null, "", cause
        );
    }

    public static AiModelException protocol(Throwable cause) {
        return new AiModelException(
                "AI_MODEL_INVALID_RESPONSE", "模型服务返回了无法识别的响应",
                false, null, "", cause
        );
    }

    public static AiModelException fromHttpStatus(int status, String requestId) {
        return switch (status) {
            case 429, 503 -> new AiModelException(
                    "AI_MODEL_BUSY", "模型服务繁忙，请稍后再试",
                    true, status, requestId, null
            );
            case 401, 403 -> new AiModelException(
                    "AI_MODEL_AUTH_ERROR", "模型服务配置错误，请联系管理员",
                    false, status, requestId, null
            );
            case 400 -> new AiModelException(
                    "AI_MODEL_REQUEST_REJECTED", "模型配置或请求参数不正确",
                    false, status, requestId, null
            );
            default -> new AiModelException(
                    "AI_MODEL_UNAVAILABLE", "模型服务暂时不可用，请稍后再试",
                    false, status, requestId, null
            );
        };
    }

    public String getCode() { return code; }
    public boolean isFallbackEligible() { return fallbackEligible; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getRequestId() { return requestId; }
}
