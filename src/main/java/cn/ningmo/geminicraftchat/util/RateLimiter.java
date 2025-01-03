package cn.ningmo.geminicraftchat.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final Map<UUID, Long> lastMessageTime;
    private final long cooldown;
    
    public RateLimiter(long cooldownMillis) {
        this.lastMessageTime = new ConcurrentHashMap<>();
        this.cooldown = cooldownMillis;
    }
    
    public boolean tryAcquire(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerId);
        
        if (lastTime == null || currentTime - lastTime >= cooldown) {
            lastMessageTime.put(playerId, currentTime);
            return true;
        }
        
        return false;
    }
    
    public long getRemainingCooldown(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerId);
        
        if (lastTime == null) {
            return 0;
        }
        
        long remaining = cooldown - (currentTime - lastTime);
        return Math.max(0, remaining);
    }
} 