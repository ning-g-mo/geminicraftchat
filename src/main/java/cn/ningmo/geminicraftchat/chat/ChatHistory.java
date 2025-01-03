package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHistory {
    private final GeminiCraftChat plugin;
    private final Map<String, LinkedList<String>> history;
    private int maxHistory;
    private boolean independentChat;

    public ChatHistory(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.history = new ConcurrentHashMap<>();
        reload();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        this.maxHistory = plugin.getConfig().getInt("chat.max_history", 10);
        this.independentChat = plugin.getConfig().getBoolean("chat.independent_chat", true);
        
        // 清理超出新限制的历史记录
        history.values().forEach(list -> {
            while (list.size() > maxHistory) {
                list.removeFirst();
            }
        });
    }

    /**
     * 添加聊天记录
     */
    public void addMessage(Player player, String message) {
        String key = getHistoryKey(player);
        history.computeIfAbsent(key, k -> new LinkedList<>());
        LinkedList<String> playerHistory = history.get(key);
        
        playerHistory.addLast(message);
        while (playerHistory.size() > maxHistory) {
            playerHistory.removeFirst();
        }
    }

    /**
     * 获取聊天历史
     */
    public List<String> getHistory(Player player) {
        String key = getHistoryKey(player);
        return new ArrayList<>(history.getOrDefault(key, new LinkedList<>()));
    }

    /**
     * 清除指定玩家的聊天历史
     */
    public void clearHistory(Player player) {
        String key = getHistoryKey(player);
        history.remove(key);
    }

    /**
     * 清除所有聊天历史
     */
    public void clearAllHistory() {
        history.clear();
    }

    /**
     * 获取历史记录键
     * 如果启用了独立对话，则使用玩家UUID，否则使用固定键
     */
    private String getHistoryKey(Player player) {
        return independentChat ? player.getUniqueId().toString() : "global";
    }
} 