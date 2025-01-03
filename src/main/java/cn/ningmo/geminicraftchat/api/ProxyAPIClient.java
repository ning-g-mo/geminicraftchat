package cn.ningmo.geminicraftchat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ProxyAPIClient {
    private final ProxyAPIConfig config;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public CompletableFuture<String> sendRequest(String model, String messages, double temperature) {
        if (!config.isEnabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("中转API未启用"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建请求体
                ObjectNode requestBody = objectMapper.createObjectNode();
                String requestFormat = config.getRequestFormat().getModel()
                    .replace("${model}", model);
                requestBody.put("model", requestFormat);

                String messagesFormat = config.getRequestFormat().getMessages()
                    .replace("${messages}", messages);
                requestBody.put("messages", messagesFormat);

                String temperatureFormat = config.getRequestFormat().getTemperature()
                    .replace("${temperature}", String.valueOf(temperature));
                requestBody.put("temperature", Double.parseDouble(temperatureFormat));

                // 构建请求
                Request.Builder requestBuilder = new Request.Builder()
                    .url(config.getUrl())
                    .method(config.getMethod(), 
                           RequestBody.create(objectMapper.writeValueAsString(requestBody), 
                           MediaType.parse("application/json")));

                // 添加请求头
                for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }

                // 发送请求
                try (Response response = client.newCall(requestBuilder.build()).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("请求失败: " + response.code());
                    }

                    String responseBody = response.body().string();
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);

                    // 检查错误
                    String errorPath = config.getResponseFormat().getErrorPath();
                    if (errorPath != null && !errorPath.isEmpty()) {
                        JsonNode errorNode = getJsonNodeByPath(jsonResponse, errorPath);
                        if (errorNode != null && !errorNode.isNull()) {
                            throw new IOException("API错误: " + errorNode.asText());
                        }
                    }

                    // 获取响应内容
                    String contentPath = config.getResponseFormat().getContentPath();
                    JsonNode contentNode = getJsonNodeByPath(jsonResponse, contentPath);
                    if (contentNode == null || contentNode.isNull()) {
                        throw new IOException("无法从响应中获取内容");
                    }

                    return contentNode.asText();
                }
            } catch (Exception e) {
                throw new RuntimeException("中转API请求失败: " + e.getMessage(), e);
            }
        });
    }

    private JsonNode getJsonNodeByPath(JsonNode root, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = root;

        for (String part : parts) {
            if (current == null) return null;

            // 处理数组访问
            if (part.contains("[") && part.contains("]")) {
                String arrayName = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(
                    part.indexOf("[") + 1, 
                    part.indexOf("]")
                ));

                current = current.path(arrayName).path(index);
            } else {
                current = current.path(part);
            }
        }

        return current;
    }

    public static ProxyAPIClient create(ProxyAPIConfig config) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

        ObjectMapper objectMapper = new ObjectMapper();
        return new ProxyAPIClient(config, client, objectMapper);
    }
} 