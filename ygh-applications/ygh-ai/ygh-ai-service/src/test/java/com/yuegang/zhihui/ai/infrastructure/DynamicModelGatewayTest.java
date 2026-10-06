package com.yuegang.zhihui.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yuegang.zhihui.system.api.InternalAiProviderConfig;
import org.junit.jupiter.api.Test;

class DynamicModelGatewayTest {
    @Test
    void refreshesConfigurationAndUsesTheCachedGatewayDuringAnOutage() {
        var configs = mock(SystemAiProviderConfigClient.class);
        var versionOne = config(1, "");
        var versionTwo = config(2, "");
        when(configs.current()).thenReturn(versionOne, versionOne, versionTwo)
                .thenThrow(new IllegalStateException("system service unavailable"));
        var gateway = new DynamicModelGateway(configs);

        assertThat(gateway.available()).isFalse();
        assertThat(gateway.modelName()).isEqualTo("unavailable");
        assertThat(gateway.supportsWebSearch()).isFalse();
        assertThat(gateway.answer("system", "question")).contains("未配置");
    }

    @Test
    void failsClosedBeforeAnyConfigurationHasBeenLoaded() {
        var configs = mock(SystemAiProviderConfigClient.class);
        when(configs.current()).thenThrow(new IllegalStateException("system service unavailable"));

        assertThat(new DynamicModelGateway(configs).available()).isFalse();
    }

    @Test
    void exposesAnAvailableGatewayForCompleteProviderConfiguration() {
        var configs = mock(SystemAiProviderConfigClient.class);
        when(configs.current()).thenReturn(config(1, "test-key"));

        assertThat(new DynamicModelGateway(configs).available()).isTrue();
    }

    private static InternalAiProviderConfig config(long version, String apiKey) {
        return new InternalAiProviderConfig(
                "DOUBAO", "https://localhost", "chat-model", "embedding-model", false, apiKey, version);
    }
}
