package cn.ningmo.geminicraftchat.npc;

import java.util.*;

public class NPCMemory {
    private final Map<String, List<Interaction>> interactions;
    
    public NPCMemory() {
        this.interactions = new HashMap<>();
    }
    
    public void addInteraction(String npcId, String playerMessage, String npcResponse) {
        List<Interaction> npcInteractions = interactions.computeIfAbsent(npcId, k -> new ArrayList<>());
        npcInteractions.add(new Interaction(playerMessage, npcResponse));
    }
    
    public List<Interaction> getInteractions(String npcId) {
        return interactions.getOrDefault(npcId, Collections.emptyList());
    }
    
    public void trim(int maxSize) {
        interactions.values().forEach(list -> {
            while (list.size() > maxSize) {
                list.remove(0);
            }
        });
    }
    
    public static class Interaction {
        private final String playerMessage;
        private final String npcResponse;
        private final long timestamp;
        
        public Interaction(String playerMessage, String npcResponse) {
            this.playerMessage = playerMessage;
            this.npcResponse = npcResponse;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getPlayerMessage() { return playerMessage; }
        public String getNpcResponse() { return npcResponse; }
        public long getTimestamp() { return timestamp; }
    }
} 