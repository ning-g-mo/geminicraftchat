package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.entity.Player;

import java.util.*;

public class ChatHistory {
    private final GeminiCraftChat plugin;
    private final Map<UUID, List<String>> history;
    private int maxHistory;

    public ChatHistory(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.history = new HashMap<>();
        reload();
    }

    public void reload() {
        maxHistory = plugin.getConfig().getInt("chat.max_history", 10);
        history.clear();
    }

    public void addMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        List<String> playerHistory = history.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        playerHistory.add(message);
        
        while (playerHistory.size() > maxHistory) {
            playerHistory.remove(0);
        }
    }

    public List<String> getHistory(Player player) {
        return history.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void clearHistory(Player player) {
        history.remove(player.getUniqueId());
    }

    public void clearAllHistory() {
        history.clear();
    }
}