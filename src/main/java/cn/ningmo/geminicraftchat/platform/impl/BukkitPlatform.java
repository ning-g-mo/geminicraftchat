package cn.ningmo.geminicraftchat.platform.impl;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.platform.Platform;
import cn.ningmo.geminicraftchat.platform.PlatformType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlatform implements Platform {
    private final GeminiCraftChat plugin;
    
    public BukkitPlatform(GeminiCraftChat plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public PlatformType getType() {
        return PlatformType.BUKKIT;
    }
    
    @Override
    public void sendMessage(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(message);
        }
    }
    
    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.hasPermission(permission);
    }
    
    @Override
    public String getPlayerName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null ? player.getName() : null;
    }
    
    @Override
    public boolean isPlayerOnline(UUID playerId) {
        return Bukkit.getPlayer(playerId) != null;
    }
    
    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
    
    @Override
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
} 