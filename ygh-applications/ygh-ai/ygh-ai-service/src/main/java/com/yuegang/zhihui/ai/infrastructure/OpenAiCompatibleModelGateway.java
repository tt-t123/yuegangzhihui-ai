package com.yuegang.zhihui.ai.infrastructure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuegang.zhihui.ai.domain.ModelAnswer;
import com.yuegang.zhihui.ai.domain.ModelGateway;
import com.yuegang.zhihui.ai.domain.ModelProviderException;
import com.yuegang.zhihui.ai.domain.ModelSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * OpenAI 兼容 API 适配器（支持 DeepSeek 等兼容 OpenAI Chat Completions 格式的供应商）。
 * API Key 仅保存在服务端配置中，不进入日志或前端响应。
 */
public final class OpenAiCompatibleModelGateway implements ModelGateway {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final RestClient client;
    private final String modelName;
    private final String chatCompletionsUrl;

    /**
     * 构造函数
     * @param baseUrl 供应商 API 基础地址（如 https://api.deepseek.com）
     * @param apiKey  API 密钥
     * @param modelName 对话模型名称（如 deepseek-chat）
     */
    public OpenAiCompatibleModelGateway(String baseUrl, String apiKey, String modelName) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("API key missing");
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("Base URL missing");
        this.modelName = modelName;
        this.chatCompletionsUrl = trimTrailingSlash(baseUrl) + "/chat/completions";
        this.client = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public String answer(String system, String user) {
        return answerWithSources(system, user).text();
    }

    @Override
    public ModelAnswer answerWithSources(String system, String user) {
        Map<?, ?> response;
        try {
            // 构建 OpenAI 兼容的 Chat Completions 请求体
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", modelName);
            request.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)
            ));
            request.put("stream", false);
            response = client.post().uri(chatCompletionsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve().body(Map.class);
        } catch (RestClientResponseException exception) {
            throw providerFailure(exception);
        } catch (RestClientException exception) {
            // 网络连接失败（DNS解析、连接超时、SSL握手等）转译为供应商异常
            throw new ModelProviderException(0, "NETWORK_FAILURE",
                    "AI 模型网络连接失败：" + exception.getMessage(), exception);
        }
        String text = extractText(response);
        if (text == null || text.isBlank()) throw new IllegalStateException("empty model response");
        return new ModelAnswer(text, extractSources(response));
    }

    @Override
    public String modelName() { return modelName; }

    @Override
    public boolean supportsWebSearch() { return false; }

    /** 去除 base URL 末尾的斜杠 */
    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** 从 OpenAI 兼容响应中提取文本内容 */
    private static String extractText(Map<?, ?> response) {
        if (response == null) return null;
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> choiceList) || choiceList.isEmpty()) return null;
        Object firstChoice = choiceList.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) return null;
        Object message = choiceMap.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) return null;
        Object content = messageMap.get("content");
        return content instanceof String text && !text.isBlank() ? text : null;
    }

    /** 从响应中提取来源信息（OpenAI 兼容格式通常无来源，保留兼容逻辑） */
    private static List<ModelSource> extractSources(Map<?, ?> response) {
        if (response == null) return List.of();
        Map<String, ModelSource> sources = new LinkedHashMap<>();
        collectSources(JSON.valueToTree(response), sources);
        return new java.util.ArrayList<>(sources.values()).stream().limit(8).toList();
    }

    /** 递归收集来源信息 */
    private static void collectSources(JsonNode node, Map<String, ModelSource> sources) {
        if (node == null || node.isNull() || sources.size() >= 8) return;
        if (node.isObject()) {
            String url = node.path("url").asText("");
            if ((url.startsWith("https://") || url.startsWith("http://")) && !sources.containsKey(url)) {
                String title = node.path("title").asText("互联网来源");
                String excerpt = node.path("snippet").asText(node.path("text").asText("公开网络检索结果"));
                sources.put(url, new ModelSource(title.isBlank() ? "互联网来源" : title,
                        excerpt.isBlank() ? "公开网络检索结果" : excerpt, url));
            }
            node.elements().forEachRemaining(child -> collectSources(child, sources));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> collectSources(child, sources));
        }
    }

    /** 构造供应商异常 */
    private static ModelProviderException providerFailure(RestClientResponseException exception) {
        String code = "UNKNOWN";
        String message = "未返回错误说明";
        try {
            JsonNode body = JSON.readTree(exception.getResponseBodyAsString());
            JsonNode error = body.path("error");
            if (error.isMissingNode() || error.isNull()) error = body;
            code = error.path("code").asText(code);
            message = error.path("message").asText(message);
        } catch (Exception ignored) {
            // 非 JSON 错误页只透传 HTTP 状态
        }
        return new ModelProviderException(exception.getStatusCode().value(), code, message, exception);
    }
}
