package com.ohcp.project1.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 极简「智能体」客户端：调用任意 OpenAI 兼容接口（DeepSeek / 通义 / Kimi / 本地 Ollama 均可）。
 *
 * <p>密钥走环境变量，绝不写进代码——提交到 GitHub 会被判违规，也是常识性事故。
 * 赛前在系统环境变量里设好：
 * <pre>
 *   OHCP_LLM_API_KEY   = 你的 key
 *   OHCP_LLM_BASE_URL  = https://api.deepseek.com     （可选）
 *   OHCP_LLM_MODEL     = deepseek-chat                （可选）
 * </pre>
 * 用本地 Ollama 时 base url 填 http://localhost:11434/v1，key 随便填。
 */
public class AgentClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public AgentClient() {
        this(System.getenv("OHCP_LLM_BASE_URL"), System.getenv("OHCP_LLM_API_KEY"),
                System.getenv("OHCP_LLM_MODEL"));
    }

    public AgentClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.deepseek.com" : baseUrl;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? "deepseek-chat" : model;
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 单轮对话，返回模型输出文本。 */
    public String chat(String prompt) throws IOException, InterruptedException {
        if (!configured()) {
            throw new IllegalStateException("未配置 OHCP_LLM_API_KEY 环境变量");
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.putArray("messages")
                .addObject()
                .put("role", "user")
                .put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("接口返回 " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText("");
    }
}
