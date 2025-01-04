package cn.ningmo.geminicraftchat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import cn.ningmo.geminicraftchat.response.GeminiResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DirectAPIClient {
    private final String apiKey;
    private final int maxRetries;
    private final long retryDelay;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public DirectAPIClient(String apiKey, int maxRetries, long retryDelay, OkHttpClient client, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public GeminiResponse sendRequest(String model, String messages, double temperature) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": %s
                    }]
                }],
                "generationConfig": {
                    "temperature": %.2f,
                    "maxOutputTokens": 1024,
                    "topP": 0.95,
                    "topK": 40
                }
            }""", 
            objectMapper.valueToTree(messages).toString(),
            temperature
        );

        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    String error = response.body() != null ? response.body().string() : "Unknown error";
                    if (attempt < maxRetries) {
                        TimeUnit.MILLISECONDS.sleep(retryDelay);
                        continue;
                    }
                    return GeminiResponse.error("API请求失败: " + error);
                }

                JsonNode jsonResponse = objectMapper.readTree(response.body().string());
                JsonNode candidates = jsonResponse.path("candidates");
                if (candidates.isEmpty()) {
                    return GeminiResponse.error("API未返回有效响应");
                }

                String content = candidates.path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

                return GeminiResponse.success(content);

            } catch (IOException e) {
                if (attempt < maxRetries) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return GeminiResponse.error("请求被中断: " + e.getMessage());
                    }
                    continue;
                }
                return GeminiResponse.error("请求失败: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return GeminiResponse.error("请求被中断: " + e.getMessage());
            }
        }

        return GeminiResponse.error("已达到最大重试次数");
    }

    public static DirectAPIClient create(String apiKey, int maxRetries, long retryDelay) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
            
        ObjectMapper objectMapper = new ObjectMapper();
        
        return new DirectAPIClient(apiKey, maxRetries, retryDelay, client, objectMapper);
    }
}