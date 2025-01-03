package cn.ningmo.geminicraftchat.api;

import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProxyAPIConfig {
    private boolean enabled;
    private String url;
    private String method;
    private Map<String, String> headers;
    private RequestFormat requestFormat;
    private ResponseFormat responseFormat;

    @Data
    public static class RequestFormat {
        private String model;
        private String messages;
        private String temperature;
    }

    @Data
    public static class ResponseFormat {
        private String contentPath;
        private String errorPath;
    }

    public static ProxyAPIConfig fromConfig(ConfigurationSection config) {
        if (config == null) return null;

        ProxyAPIConfig proxyConfig = new ProxyAPIConfig();
        proxyConfig.setEnabled(config.getBoolean("enabled", false));
        proxyConfig.setUrl(config.getString("url", ""));
        proxyConfig.setMethod(config.getString("method", "POST"));

        // 加载请求头
        Map<String, String> headers = new HashMap<>();
        ConfigurationSection headersSection = config.getConfigurationSection("headers");
        if (headersSection != null) {
            for (String key : headersSection.getKeys(false)) {
                String value = headersSection.getString(key);
                if (value != null) {
                    // 处理环境变量
                    if (value.contains("${")) {
                        value = processEnvironmentVariables(value);
                    }
                    headers.put(key, value);
                }
            }
        }
        proxyConfig.setHeaders(headers);

        // 加载请求格式
        RequestFormat requestFormat = new RequestFormat();
        ConfigurationSection requestFormatSection = config.getConfigurationSection("request_format");
        if (requestFormatSection != null) {
            requestFormat.setModel(requestFormatSection.getString("model", "${model}"));
            requestFormat.setMessages(requestFormatSection.getString("messages", "${messages}"));
            requestFormat.setTemperature(requestFormatSection.getString("temperature", "${temperature}"));
        }
        proxyConfig.setRequestFormat(requestFormat);

        // 加载响应格式
        ResponseFormat responseFormat = new ResponseFormat();
        ConfigurationSection responseFormatSection = config.getConfigurationSection("response_format");
        if (responseFormatSection != null) {
            responseFormat.setContentPath(responseFormatSection.getString("content_path", "choices[0].message.content"));
            responseFormat.setErrorPath(responseFormatSection.getString("error_path", "error.message"));
        }
        proxyConfig.setResponseFormat(responseFormat);

        return proxyConfig;
    }

    private static String processEnvironmentVariables(String value) {
        if (value == null) return null;
        
        while (value.contains("${")) {
            int start = value.indexOf("${");
            int end = value.indexOf("}", start);
            if (end == -1) break;
            
            String envVar = value.substring(start + 2, end);
            String envValue = System.getenv(envVar);
            if (envValue != null) {
                value = value.replace("${" + envVar + "}", envValue);
            } else {
                // 如果环境变量不存在，保持原样
                break;
            }
        }
        return value;
    }
} 