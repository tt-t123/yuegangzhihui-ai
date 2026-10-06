package com.yuegang.zhihui.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelProviderExceptionTest {
    @Test
    void mapsProviderFailuresToActionableUserMessages() {
        assertThat(error(400, "ModelNotOpen").userMessage()).contains("模型尚未开通");
        assertThat(error(404, "ModelNotFound").userMessage()).contains("未找到当前模型");
        assertThat(error(404, "EndpointNotFound").userMessage()).contains("未找到当前模型");
        assertThat(error(401, "Denied").userMessage()).contains("API Key 无效");
        assertThat(error(403, "Denied").userMessage()).contains("API Key 无效");
        assertThat(error(400, "AuthFailed").userMessage()).contains("API Key 无效");
        assertThat(error(400, "BalanceInsufficient").userMessage()).contains("额度或余额不足");
        assertThat(error(400, "QuotaExceeded").userMessage()).contains("额度或余额不足");
        assertThat(error(400, "AccountOverdue").userMessage()).contains("额度或余额不足");
        assertThat(error(429, "Busy").userMessage()).contains("请求频率超限");
        assertThat(error(400, "RateLimitExceeded").userMessage()).contains("请求频率超限");
        assertThat(error(500, "InternalError").userMessage()).contains("HTTP 500", "InternalError");
    }

    @Test
    void suppliesFallbacksAndSanitizesProviderMessages() {
        var fallback = new ModelProviderException(500, null, " ", null);
        assertThat(fallback.providerCode()).isEqualTo("UNKNOWN");
        assertThat(fallback.providerMessage()).isEqualTo("未返回错误说明");

        var sanitized = new ModelProviderException(500, "InternalError",
                "Bearer secret.token\tsk-private account 987654321 request id: req-secret\n", null);
        assertThat(sanitized.providerMessage())
                .doesNotContain("secret.token", "sk-private", "987654321", "req-secret", "\n", "\t")
                .contains("Bearer [REDACTED]", "account [REDACTED]", "Request id: [REDACTED]");

        var longMessage = new ModelProviderException(500, "InternalError", "x".repeat(301), null);
        assertThat(longMessage.providerMessage()).hasSize(300);
    }

    private static ModelProviderException error(int status, String code) {
        return new ModelProviderException(status, code, "provider message", null);
    }
}
