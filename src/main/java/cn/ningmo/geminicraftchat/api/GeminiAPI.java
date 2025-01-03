package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.response.GeminiResponse;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.CompletableFuture;

public class GeminiAPI {
    private final GeminiCraftChat plugin;
    @Getter
    private final String model;
    private final double temperature;
    private final ProxyAPIClient proxyClient;
    private final DirectAPIClient directClient;

    public GeminiAPI(GeminiCraftChat plugin) {
        this.plugin = plugin;
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("api");
        this.model = config.getString("model", "gemini-pro");
        this.temperature = config.getDouble("generation.temperature", 0.7);

        // 初始化中转API客户端
        ConfigurationSection proxyConfig = config.getConfigurationSection("proxy");
        if (proxyConfig != null) {
            ProxyAPIConfig proxyAPIConfig = ProxyAPIConfig.fromConfig(proxyConfig);
            this.proxyClient = ProxyAPIClient.create(proxyAPIConfig);
        } else {
            this.proxyClient = null;
        }

        // 初始化直连API客户端
        this.directClient = DirectAPIClient.create(config);
    }

    public CompletableFuture<GeminiResponse> chatAsync(String message, String persona) {
        // 如果中转API可用且启用，优先使用中转API
        if (proxyClient != null && plugin.getConfig().getBoolean("api.proxy.enabled", false)) {
            return proxyClient.sendRequest(model, message, temperature)
                .thenApply(response -> new GeminiResponse(response, null));
        }

        // 否则使用直连API
        return directClient.sendRequest(model, message, temperature, persona);
    }
} 