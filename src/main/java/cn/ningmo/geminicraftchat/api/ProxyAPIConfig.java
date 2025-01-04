package cn.ningmo.geminicraftchat.api;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

public class ProxyAPIConfig {
    private boolean enabled;
    private String url;
    private String method;
    private Map<String, String> headers;
    private RequestFormat requestFormat;
    private ResponseFormat responseFormat;

    public static class RequestFormat {
        private String model;
        private String messages;
        private String temperature;

        // Getters and Setters
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getMessages() { return messages; }
        public void setMessages(String messages) { this.messages = messages; }
        public String getTemperature() { return temperature; }
        public void setTemperature(String temperature) { this.temperature = temperature; }
    }

    public static class ResponseFormat {
        private String contentPath;
        private String errorPath;

        // Getters and Setters
        public String getContentPath() { return contentPath; }
        public void setContentPath(String contentPath) { this.contentPath = contentPath; }
        public String getErrorPath() { return errorPath; }
        public void setErrorPath(String errorPath) { this.errorPath = errorPath; }
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public RequestFormat getRequestFormat() { return requestFormat; }
    public void setRequestFormat(RequestFormat requestFormat) { this.requestFormat = requestFormat; }
    public ResponseFormat getResponseFormat() { return responseFormat; }
    public void setResponseFormat(ResponseFormat responseFormat) { this.responseFormat = responseFormat; }

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