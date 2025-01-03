package cn.ningmo.geminicraftchat.npc;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.api.GeminiResponse;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NPCManager {
    private final GeminiCraftChat plugin;
    private final Map<String, NPC> npcs;
    private final Map<UUID, NPCMemory> memories;
    private final Random random;
    
    public NPCManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.npcs = new HashMap<>();
        this.memories = new HashMap<>();
        this.random = new Random();
        loadNPCs();
    }
    
    private void loadNPCs() {
        ConfigurationSection npcSection = plugin.getConfig().getConfigurationSection("npc.list");
        if (npcSection == null) return;
        
        for (String id : npcSection.getKeys(false)) {
            ConfigurationSection section = npcSection.getConfigurationSection(id);
            if (section == null) continue;
            
            NPC npc = new NPC(id, section);
            npcs.put(id, npc);
        }
    }
    
    public Optional<NPC> tryTriggerNPC(String message) {
        int chance = plugin.getConfig().getInt("npc.trigger_chance", 15);
        if (random.nextInt(100) >= chance) {
            return Optional.empty();
        }
        
        return npcs.values().stream()
            .filter(npc -> npc.shouldTrigger(message))
            .findFirst();
    }
    
    public CompletableFuture<String> generateResponse(UUID playerId, NPC npc, String message) {
        List<NPCMemory.Interaction> history = getMemory(playerId, npc.getId());
        String prompt = npc.getPrompt(message, history);
        
        return plugin.getGeminiAPI().chatAsync(prompt, "npc")
            .thenApply(response -> {
                if (response.isSuccess()) {
                    String aiResponse = response.getMessage();
                    addMemory(playerId, npc.getId(), message, aiResponse);
                    return aiResponse;
                } else {
                    return "抱歉，我现在有点累，稍后再聊吧。";
                }
            });
    }
    
    public void addMemory(UUID playerId, String npcId, String playerMessage, String npcResponse) {
        if (!plugin.getConfig().getBoolean("npc.enable_memory", true)) {
            return;
        }
        
        NPCMemory memory = memories.computeIfAbsent(playerId, k -> new NPCMemory());
        memory.addInteraction(npcId, playerMessage, npcResponse);
        
        int maxMemory = plugin.getConfig().getInt("npc.max_memory", 20);
        memory.trim(maxMemory);
    }
    
    public List<NPCMemory.Interaction> getMemory(UUID playerId, String npcId) {
        NPCMemory memory = memories.get(playerId);
        return memory != null ? memory.getInteractions(npcId) : Collections.emptyList();
    }
    
    public void clearMemory(UUID playerId) {
        memories.remove(playerId);
    }
} 