package cn.ningmo.geminicraftchat.platform.impl;

import cn.ningmo.geminicraftchat.platform.Platform;
import cn.ningmo.geminicraftchat.platform.PlatformType;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityPlatform implements Platform {
    private final ProxyServer server;
    private final Object plugin;
    
    public VelocityPlatform(ProxyServer server, Object plugin) {
        this.server = server;
        this.plugin = plugin;
    }
    
    @Override
    public PlatformType getType() {
        return PlatformType.VELOCITY;
    }
    
    @Override
    public void sendMessage(UUID playerId, String message) {
        Optional<Player> player = server.getPlayer(playerId);
        player.ifPresent(p -> p.sendMessage(Component.text(message)));
    }
    
    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        Optional<Player> player = server.getPlayer(playerId);
        return player.map(p -> p.hasPermission(permission)).orElse(false);
    }
    
    @Override
    public String getPlayerName(UUID playerId) {
        Optional<Player> player = server.getPlayer(playerId);
        return player.map(Player::getUsername).orElse(null);
    }
    
    @Override
    public boolean isPlayerOnline(UUID playerId) {
        return server.getPlayer(playerId).isPresent();
    }
    
    @Override
    public void runAsync(Runnable task) {
        server.getScheduler()
            .buildTask(plugin, task)
            .schedule();
    }
    
    @Override
    public void runSync(Runnable task) {
        // Velocity 没有主线程的概念，所以这里直接执行
        task.run();
    }
} 