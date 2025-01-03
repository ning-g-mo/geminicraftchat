package cn.ningmo.geminicraftchat.chat;

import java.util.*;

public class ChatHistory {
    private final int maxHistory;
    private final Map<UUID, List<ChatMessage>> playerHistory;
    private final boolean independentChat;
    
    public ChatHistory(int maxHistory, boolean independentChat) {
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
        if (!independentChat) {
            // 如果不是独立对话，返回所有玩家的聊天记录合并
            List<ChatMessage> allHistory = new ArrayList<>();
            playerHistory.values().forEach(allHistory::addAll);
            allHistory.sort(Comparator.comparingLong(ChatMessage::getTimestamp));
            return allHistory.subList(Math.max(0, allHistory.size() - maxHistory), allHistory.size());
        }
        return playerHistory.getOrDefault(playerId, new ArrayList<>());
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