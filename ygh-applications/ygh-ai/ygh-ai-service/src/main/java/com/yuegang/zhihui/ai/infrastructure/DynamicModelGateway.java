package com.yuegang.zhihui.ai.infrastructure;

import com.yuegang.zhihui.ai.domain.ModelGateway;
import com.yuegang.zhihui.ai.domain.ModelAnswer;
import com.yuegang.zhihui.system.api.InternalAiProviderConfig;

public final class DynamicModelGateway implements ModelGateway {
    private final SystemAiProviderConfigClient configs;
    private volatile Cached cached;

    public DynamicModelGateway(SystemAiProviderConfigClient configs) { this.configs = configs; }
    @Override public String answer(String systemPrompt, String userPrompt) { return delegate().answer(systemPrompt, userPrompt); }
    @Override public ModelAnswer answerWithSources(String systemPrompt, String userPrompt) { return delegate().answerWithSources(systemPrompt, userPrompt); }
    @Override public String modelName() { return delegate().modelName(); }
    @Override public boolean available() { return delegate().available(); }
    @Override public boolean supportsWebSearch() { return delegate().supportsWebSearch(); }

    private ModelGateway delegate() {
        InternalAiProviderConfig current;
        try { current = configs.current(); }
        catch (RuntimeException unavailable) {
            Cached value = cached;
            return value == null ? new UnavailableModelGateway() : value.gateway();
        }
        Cached value = cached;
        if (value != null && value.version() == current.version()) return value.gateway();
        ModelGateway gateway = current.configured()
                ? createGateway(current)
                : new UnavailableModelGateway();
        cached = new Cached(current.version(), gateway);
        return gateway;
    }

    /**
     * 根据 provider 标识创建对应的模型网关
     * @param config AI 供应商配置
     * @return 模型网关实例
     */
    private static ModelGateway createGateway(InternalAiProviderConfig config) {
        String provider = config.provider() == null ? "" : config.provider().toUpperCase();
        // DeepSeek 及其他 OpenAI 兼容供应商使用 OpenAiCompatibleModelGateway
        if (provider.startsWith("DEEPSEEK") || provider.startsWith("OPENAI")) {
            return new OpenAiCompatibleModelGateway(config.baseUrl(), config.apiKey(), config.chatModel());
        }
        // 豆包方舟使用 DoubaoModelGateway（Responses API）
        return new DoubaoModelGateway(config.baseUrl(), config.apiKey(), config.chatModel(),
                config.webSearchEnabled());
    }
    private record Cached(long version, ModelGateway gateway) {}
}
