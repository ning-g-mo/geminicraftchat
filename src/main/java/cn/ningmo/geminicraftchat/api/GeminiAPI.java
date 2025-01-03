package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.response.GeminiResponse;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.CompletableFuture;

public class GeminiAPI {
    private final GeminiCraftChat plugin;
    @Getter
    private String model;
    private double temperature;
    private ProxyAPIClient proxyClient;
    private DirectAPIClient directClient;

    public GeminiAPI(GeminiCraftChat plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 重新加载API配置
     */
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
            this.directClient = DirectAPIClient.create(config);
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("直连API初始化失败: " + e.getMessage());
            if (this.proxyClient == null) {
                throw e; // 如果两种API都无法使用，抛出异常
            }
        }
    }

    public CompletableFuture<GeminiResponse> chatAsync(String message, String persona) {
        // 如果中转API可用且启用，优先使用中转API
        if (proxyClient != null && plugin.getConfig().getBoolean("api.proxy.enabled", false)) {
            return proxyClient.sendRequest(model, message, temperature)
                .thenApply(response -> new GeminiResponse(response, null));
        }

        // 检查直连API是否可用
        if (directClient == null) {
            return CompletableFuture.completedFuture(
                GeminiResponse.error("API客户端未初始化，请检查配置"));
        }

        // 使用直连API
        return directClient.sendRequest(model, message, temperature, persona);
    }

    /**
     * 设置模型并保存到配置文件
     */
    public void setModel(String model) {
        this.model = model;
        plugin.getConfig().set("api.model", model);
        plugin.saveConfig();
    }
} 