package com.yuegang.zhihui.search.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yuegang.zhihui.system.api.InternalAiProviderConfig;
import org.junit.jupiter.api.Test;

class DynamicDoubaoEmbeddingGatewayTest {
    @Test
    void refreshesChangedConfigurationAndFallsBackToTheLastKnownGateway() {
        var configs = mock(SystemAiProviderConfigClient.class);
        var versionOne = config(1);
        var versionTwo = config(2);
        when(configs.current()).thenReturn(versionOne, versionOne, versionTwo)
                .thenThrow(new IllegalStateException("system service unavailable"));
        var gateway = new DynamicDoubaoEmbeddingGateway(configs);

        assertThat(gateway.configured()).isFalse();
        assertThat(gateway.configured()).isFalse();
        assertThat(gateway.configured()).isFalse();
        assertThat(gateway.embed("policy")).hasSize(1024);
    }

    @Test
    void usesDevelopmentEmbeddingWhenConfigurationHasNeverBeenAvailable() {
        var configs = mock(SystemAiProviderConfigClient.class);
        when(configs.current()).thenThrow(new IllegalStateException("system service unavailable"));

        var gateway = new DynamicDoubaoEmbeddingGateway(configs);

        assertThat(gateway.configured()).isFalse();
        assertThat(gateway.embed("policy")).hasSize(1024);
    }

    private static InternalAiProviderConfig config(long version) {
        return new InternalAiProviderConfig(
                "DOUBAO", "https://localhost", "chat-model", "embedding-model", false, "", version);
    }
}
