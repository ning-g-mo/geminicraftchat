package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final GeminiCraftChat plugin;
    private final Map<UUID, Long> lastMessageTime;
    private final Map<UUID, Integer> messageCount;
    private long cooldown;
    private int maxMessages;
    private long resetInterval;

    public RateLimiter(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.lastMessageTime = new ConcurrentHashMap<>();
        this.messageCount = new ConcurrentHashMap<>();
        reload();
    }

    public void reload() {
        cooldown = plugin.getConfig().getLong("rate_limit.cooldown", 5000);
        maxMessages = plugin.getConfig().getInt("rate_limit.max_messages", 5);
        resetInterval = plugin.getConfig().getLong("rate_limit.reset_interval", 60000);
        
        lastMessageTime.clear();
        messageCount.clear();
    }

    public boolean tryAcquire(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // 检查冷却时间
        Long lastTime = lastMessageTime.get(playerId);
        if (lastTime != null && currentTime - lastTime < cooldown) {
            return false;
        }
        
        // 检查消息计数
        int count = messageCount.getOrDefault(playerId, 0);
        if (count >= maxMessages) {
            // 检查是否需要重置计数
            if (lastTime != null && currentTime - lastTime >= resetInterval) {
                messageCount.put(playerId, 1);
            } else {
                return false;
            }
        } else {
            messageCount.put(playerId, count + 1);
        }
        
        lastMessageTime.put(playerId, currentTime);
        return true;
    }

    public long getRemainingCooldown(Player player) {
        Long lastTime = lastMessageTime.get(player.getUniqueId());
        if (lastTime == null) {
            return 0;
        }
        
        long remaining = cooldown - (System.currentTimeMillis() - lastTime);
        return Math.max(0, remaining);
    }

    public int getRemainingMessages(Player player) {
        return maxMessages - messageCount.getOrDefault(player.getUniqueId(), 0);
    }

    public void reset(Player player) {
        UUID playerId = player.getUniqueId();
        lastMessageTime.remove(playerId);
        messageCount.remove(playerId);
    }

    public void resetAll() {
        lastMessageTime.clear();
        messageCount.clear();
    }
}