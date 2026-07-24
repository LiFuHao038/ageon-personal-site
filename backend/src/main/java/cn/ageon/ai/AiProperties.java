package cn.ageon.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ageon.ai")
public class AiProperties {
    private String apiKey = "";
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String primaryModel = "qwen-plus";
    private boolean fallbackEnabled = true;
    private String fallbackModel = "kimi/kimi-k3";
    private int connectTimeoutSeconds = 10;
    private int responseTimeoutSeconds = 120;
    private int contextWindowTokens = 128_000;
    private int maxOutputTokens = 2_000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getPrimaryModel() { return primaryModel; }
    public void setPrimaryModel(String primaryModel) { this.primaryModel = primaryModel; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getResponseTimeoutSeconds() { return responseTimeoutSeconds; }
    public void setResponseTimeoutSeconds(int responseTimeoutSeconds) { this.responseTimeoutSeconds = responseTimeoutSeconds; }
    public int getContextWindowTokens() { return contextWindowTokens; }
    public void setContextWindowTokens(int contextWindowTokens) { this.contextWindowTokens = contextWindowTokens; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
}
