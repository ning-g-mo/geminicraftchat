package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class GeminiAPI {
    private final GeminiCraftChat plugin;
    private String currentModel;
    private String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRIES = 3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    public GeminiAPI(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.objectMapper = new ObjectMapper();
        loadConfig();
    }
    
    private void loadConfig() {
        this.currentModel = plugin.getConfig().getString("api.model");
        this.apiKey = System.getenv("GEMINI_API_KEY");
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            this.apiKey = plugin.getConfig().getString("api.key");
        }
    }
    
    public CompletableFuture<GeminiResponse> chatAsync(String message, String persona) {
        if (message == null || message.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                GeminiResponse.error("消息不能为空"));
        }
        
        ConfigurationSection personaConfig = plugin.getConfig()
            .getConfigurationSection("personas." + persona);
            
        if (personaConfig == null) {
            return CompletableFuture.completedFuture(
                GeminiResponse.error("找不到指定的人设：" + persona));
        }
        
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append(personaConfig.getString("context")).append("\n\n");
            prompt.append("用户: ").append(message).append("\n");
            prompt.append("助手: ");
            
            return sendRequestWithRetry(prompt.toString(), MAX_RETRIES);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                GeminiResponse.error("处理请求时出错：" + e.getMessage()));
        }
    }
    
    private CompletableFuture<GeminiResponse> sendRequestWithRetry(String prompt, int retriesLeft) {
        return sendRequest(prompt)
            .thenApply(response -> {
                try {
                    JsonNode root = objectMapper.readTree(response);
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        String content = candidates.get(0)
                            .path("content")
                            .path("parts")
                            .get(0)
                            .path("text")
                            .asText();
                        return GeminiResponse.success(content);
                    }
                    return GeminiResponse.error("无法解析API响应");
                } catch (Exception e) {
                    throw new RuntimeException("解析响应失败", e);
                }
            })
            .exceptionally(e -> {
                if (retriesLeft > 0) {
                    return sendRequestWithRetry(prompt, retriesLeft - 1)
                        .join();
                }
                return GeminiResponse.error("API调用失败：" + e.getMessage());
            });
    }
    
    private CompletableFuture<String> sendRequest(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("API密钥未配置");
        }
        
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateText", 
            currentModel);
            
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.7,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 1024,
                    "stopSequences": []
                },
                "safetySettings": []
            }""", 
            prompt.replace("\"", "\\\"").replace("\n", "\\n"));
            
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
                
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("API返回错误状态码：" + response.statusCode() + 
                            "\n响应内容：" + response.body());
                    }
                    return response.body();
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new RuntimeException("构建API请求时出错", e));
        }
    }
    
    public void setModel(String model) {
        this.currentModel = model;
        plugin.getConfig().set("api.model", model);
        plugin.saveConfig();
    }
    
    public String getCurrentModel() {
        return currentModel;
    }
} 