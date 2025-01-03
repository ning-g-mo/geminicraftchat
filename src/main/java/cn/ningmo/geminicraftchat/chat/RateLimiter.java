package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final GeminiCraftChat plugin;
    private final Map<String, Long> lastUsage;
    private long cooldown;
    private int maxRequests;

    public RateLimiter(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.lastUsage = new ConcurrentHashMap<>();
        reload();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        this.cooldown = plugin.getConfig().getLong("chat.cooldown", 10000);
        this.maxRequests = plugin.getConfig().getInt("api.rate_limit.max_requests", 60);
    }

    /**
     * 检查是否可以发送消息
     */
    public boolean canSendMessage(Player player) {
        if (player.hasPermission("gcc.bypass_cooldown")) {
            return true;
        }

        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastUsage.get(playerId);

        if (lastTime == null || currentTime - lastTime >= cooldown) {
            lastUsage.put(playerId, currentTime);
            return true;
        }

        return false;
    }

    /**
     * 获取剩余冷却时间（毫秒）
     */
    public long getRemainingCooldown(Player player) {
        if (player.hasPermission("gcc.bypass_cooldown")) {
            return 0;
        }

        String playerId = player.getUniqueId().toString();
        Long lastTime = lastUsage.get(playerId);

        if (lastTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long remaining = cooldown - (currentTime - lastTime);
        return Math.max(0, remaining);
    }

    /**
     * 重置玩家的冷却时间
     */
    public void resetCooldown(Player player) {
        lastUsage.remove(player.getUniqueId().toString());
    }

    /**
     * 获取当前冷却时间
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * 获取每分钟最大请求数
     */
    public int getMaxRequests() {
        return maxRequests;
    }
} 