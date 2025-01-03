package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import java.util.*;

public class ChatHistory {
    private final int maxHistory;
    private final Map<UUID, List<ChatMessage>> playerHistory;
    private final boolean independentChat;
    private final GeminiCraftChat plugin;
    
    public ChatHistory(GeminiCraftChat plugin, int maxHistory, boolean independentChat) {
        this.plugin = plugin;
        this.maxHistory = maxHistory;
        this.independentChat = independentChat;
        this.playerHistory = new HashMap<>();
    }
    
    public void addMessage(UUID playerId, String userMessage, String aiResponse) {
        List<ChatMessage> history = playerHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        history.add(new ChatMessage(userMessage, aiResponse));
        
        while (history.size() > maxHistory) {
            history.remove(0);
        }
    }
    
    public List<ChatMessage> getHistory(UUID playerId) {
        List<ChatMessage> history;
        if (!independentChat) {
            // 如果不是独立对话，返回所有玩家的聊天记录合并
            List<ChatMessage> allHistory = new ArrayList<>();
            playerHistory.values().forEach(allHistory::addAll);
            allHistory.sort(Comparator.comparingLong(ChatMessage::getTimestamp));
            history = allHistory.subList(Math.max(0, allHistory.size() - maxHistory), allHistory.size());
        } else {
            history = playerHistory.getOrDefault(playerId, new ArrayList<>());
        }
        
        // 记录调试日志
        if (plugin.getDebugLogger().isLogChatHistory()) {
            StringBuilder context = new StringBuilder();
            for (ChatMessage msg : history) {
                context.append("用户: ").append(msg.getUserMessage()).append("\n");
                context.append("AI: ").append(msg.getAiResponse()).append("\n");
            }
            plugin.getDebugLogger().logChatHistory(playerId.toString(), context.toString());
        }
        
        return history;
    }
    
    public void clearHistory(UUID playerId) {
        playerHistory.remove(playerId);
    }
    
    public void clearAllHistory() {
        playerHistory.clear();
    }
    
    public static class ChatMessage {
        private final String userMessage;
        private final String aiResponse;
        private final long timestamp;
        
        public ChatMessage(String userMessage, String aiResponse) {
            this.userMessage = userMessage;
            this.aiResponse = aiResponse;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getUserMessage() { return userMessage; }
        public String getAiResponse() { return aiResponse; }
        public long getTimestamp() { return timestamp; }
    }
} 