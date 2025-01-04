package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.response.GeminiResponse;
import org.bukkit.configuration.ConfigurationSection;

public class GeminiAPI {
    private final GeminiCraftChat plugin;
    private String model;
    private double temperature;
    private ProxyAPIClient proxyClient;
    private DirectAPIClient directClient;

    public GeminiAPI(GeminiCraftChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("api");
        if (config == null) {
            throw new IllegalStateException("配置文件中缺少 api 部分");
        }

        this.model = config.getString("model", "gemini-pro");
        this.temperature = config.getDouble("generation.temperature", 0.7);

        // 重新初始化中转API客户端
        ConfigurationSection proxyConfig = config.getConfigurationSection("proxy");
        if (proxyConfig != null) {
            ProxyAPIConfig proxyAPIConfig = ProxyAPIConfig.fromConfig(proxyConfig);
            if (proxyAPIConfig != null) {
                this.proxyClient = ProxyAPIClient.create(proxyAPIConfig);
            }
        } else {
            this.proxyClient = null;
        }

        // 重新初始化直连API客户端
        try {
            String apiKey = config.getString("key");
            int maxRetries = config.getInt("max_retries", 3);
            long retryDelay = config.getLong("retry_delay", 1000);
            
            if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
                String envApiKey = System.getenv("GEMINI_API_KEY");
                if (envApiKey != null && !envApiKey.isEmpty()) {
                    apiKey = envApiKey;
                } else {
                    throw new IllegalStateException("未设置API密钥");
                }
            }
            
            this.directClient = DirectAPIClient.create(apiKey, maxRetries, retryDelay);
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("直连API初始化失败: " + e.getMessage());
            if (this.proxyClient == null) {
                throw e; // 如果两种API都无法使用，抛出异常
            }
        }
    }

    public GeminiResponse chat(String message) {
        // 如果中转API可用且启用，优先使用中转API
        if (proxyClient != null && plugin.getConfig().getBoolean("api.proxy.enabled", false)) {
            return proxyClient.sendRequest(model, message, temperature);
        }

        // 检查直连API是否可用
        if (directClient == null) {
            return GeminiResponse.error("API客户端未初始化，请检查配置");
        }

        // 使用直连API
        return directClient.sendRequest(model, message, temperature);
    }

    public void setModel(String model) {
        this.model = model;
        plugin.getConfig().set("api.model", model);
        plugin.saveConfig();
    }

    public String getModel() {
        return model;
    }
}