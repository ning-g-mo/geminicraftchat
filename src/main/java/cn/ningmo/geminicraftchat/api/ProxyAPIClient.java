package cn.ningmo.geminicraftchat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import cn.ningmo.geminicraftchat.response.GeminiResponse;

import java.io.IOException;
import java.util.Map;

public class ProxyAPIClient {
    private final ProxyAPIConfig config;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public ProxyAPIClient(ProxyAPIConfig config, OkHttpClient client, ObjectMapper objectMapper) {
        this.config = config;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public GeminiResponse sendRequest(String model, String messages, double temperature) {
        try {
            // 构建请求体
            String requestBody = buildRequestBody(model, messages, temperature);

            // 构建请求
            Request.Builder requestBuilder = new Request.Builder()
                .url(config.getUrl())
                .method(config.getMethod(), 
                    RequestBody.create(requestBody, MediaType.parse("application/json")));

            // 添加请求头
            for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }

            // 发送请求
            Response response = client.newCall(requestBuilder.build()).execute();
            if (!response.isSuccessful()) {
                String error = response.body() != null ? response.body().string() : "Unknown error";
                return GeminiResponse.error("API request failed: " + error);
            }

            // 解析响应
            JsonNode jsonResponse = objectMapper.readTree(response.body().string());

            // 检查错误
            String errorPath = config.getResponseFormat().getErrorPath();
            if (errorPath != null && !errorPath.isEmpty()) {
                JsonNode errorNode = getJsonNodeByPath(jsonResponse, errorPath);
                if (errorNode != null && !errorNode.isMissingNode()) {
                    return GeminiResponse.error(errorNode.asText());
                }
            }

            // 获取内容
            String contentPath = config.getResponseFormat().getContentPath();
            JsonNode contentNode = getJsonNodeByPath(jsonResponse, contentPath);
            if (contentNode == null || contentNode.isMissingNode()) {
                return GeminiResponse.error("No content found in response");
            }

            return GeminiResponse.success(contentNode.asText());

        } catch (IOException e) {
            return GeminiResponse.error("Request failed: " + e.getMessage());
        }
    }

    private String buildRequestBody(String model, String messages, double temperature) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();

        String modelFormat = config.getRequestFormat().getModel();
        String messagesFormat = config.getRequestFormat().getMessages();
        String temperatureFormat = config.getRequestFormat().getTemperature();

        if (modelFormat != null) {
            requestBody.put(modelFormat.replace("${model}", ""), model);
        }
        if (messagesFormat != null) {
            requestBody.put(messagesFormat.replace("${messages}", ""), messages);
        }
        if (temperatureFormat != null) {
            requestBody.put(temperatureFormat.replace("${temperature}", ""), temperature);
        }

        return objectMapper.writeValueAsString(requestBody);
    }

    private JsonNode getJsonNodeByPath(JsonNode root, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        JsonNode current = root;
        for (String part : path.split("\\.")) {
            if (part.contains("[") && part.contains("]")) {
                String arrayName = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                current = current.path(arrayName).path(index);
            } else {
                current = current.path(part);
            }
            if (current.isMissingNode()) {
                return null;
            }
        }
        return current;
    }

    public static ProxyAPIClient create(ProxyAPIConfig config) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
            
        ObjectMapper objectMapper = new ObjectMapper();
        
        return new ProxyAPIClient(config, client, objectMapper);
    }
}