package com.yuegang.zhihui.system.api;

/**
 * 内部使用的 AI 提供商完整配置对象
 */
public record InternalAiProviderConfig(
        String provider,               // 供应商标识
        String baseUrl,                // 基础地址
        String chatModel,              // 对话模型
        String embeddingModel,         // 向量模型
        boolean webSearchEnabled,      // 是否启用联网搜索
        String apiKey,                 // 明文 API 密钥
        long version                   // 版本号
) {
    /**
     * 检查配置是否已完成（所有必填项是否均已填写）
     */
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank()
                && chatModel != null && !chatModel.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }
}

