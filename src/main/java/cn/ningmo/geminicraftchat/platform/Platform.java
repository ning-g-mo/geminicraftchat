package cn.ningmo.geminicraftchat.platform;

import java.util.UUID;

public interface Platform {
    /**
     * 获取平台类型
     */
    PlatformType getType();
    
    /**
     * 发送消息给玩家
     */
    void sendMessage(UUID playerId, String message);
    
    /**
     * 检查权限
     */
    boolean hasPermission(UUID playerId, String permission);
    
    /**
     * 获取玩家名称
     */
    String getPlayerName(UUID playerId);
    
    /**
     * 检查玩家是否在线
     */
    boolean isPlayerOnline(UUID playerId);
    
    /**
     * 异步执行任务
     */
    void runAsync(Runnable task);
    
    /**
     * 在主线程执行任务
     */
    void runSync(Runnable task);
} 