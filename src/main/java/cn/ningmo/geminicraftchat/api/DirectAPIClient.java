package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.response.GeminiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class DirectAPIClient {
    private final String apiKey;
    private final int maxRetries;
    private final long retryDelay;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public CompletableFuture<GeminiResponse> sendRequest(String model, String message, double temperature, String persona) {
        if (message == null || message.trim().isEmpty()) {
            return CompletableFuture.completedFuture(GeminiResponse.error("消息不能为空"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateText", model);
                String requestBody = String.format("""
                    {
                        "contents": [{
                            "parts": [{
                                "text": "%s"
                            }]
                        }],
                        "generationConfig": {
                            "temperature": %f,
                            "topK": 40,
                            "topP": 0.95,
                            "maxOutputTokens": 1024,
                            "stopSequences": []
                        },
                        "safetySettings": []
                    }""",
                    message.replace("\"", "\\\"").replace("\n", "\\n"),
                    temperature);

                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

                return executeWithRetry(request, maxRetries);
            } catch (Exception e) {
                return GeminiResponse.error("API请求失败: " + e.getMessage());
            }
        });
    }

    private GeminiResponse executeWithRetry(Request request, int retriesLeft) throws Exception {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (retriesLeft > 0) {
                    Thread.sleep(retryDelay);
                    return executeWithRetry(request, retriesLeft - 1);
                }
                return GeminiResponse.error("API返回错误状态码: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
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
            if (retriesLeft > 0) {
                Thread.sleep(retryDelay);
                return executeWithRetry(request, retriesLeft - 1);
            }
            throw e;
        }
    }

    public static DirectAPIClient create(ConfigurationSection config) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getString("key");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("API密钥未配置");
        }

        int maxRetries = config.getInt("max_retries", 3);
        long retryDelay = config.getLong("retry_delay", 1000);
        int timeout = config.getInt("timeout", 30);

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build();

        ObjectMapper objectMapper = new ObjectMapper();
        return new DirectAPIClient(apiKey, maxRetries, retryDelay, client, objectMapper);
    }
} 