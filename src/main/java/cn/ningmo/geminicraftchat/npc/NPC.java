package cn.ningmo.geminicraftchat.npc;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public class NPC {
    private final String id;
    private final String name;
    private final String description;
    private final String personality;
    private final List<String> triggerWords;
    
    public NPC(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name");
        this.description = config.getString("description");
        this.personality = config.getString("personality");
        this.triggerWords = config.getStringList("trigger_words");
    }
    
    public boolean shouldTrigger(String message) {
        return triggerWords.stream()
            .anyMatch(word -> message.toLowerCase().contains(word.toLowerCase()));
    }
    
    public String getPrompt(String playerMessage, List<NPCMemory.Interaction> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(personality).append("\n\n");
        
        // 添加历史记录上下文
        if (!history.isEmpty()) {
            prompt.append("最近的对话记录：\n");
            for (NPCMemory.Interaction interaction : history) {
                prompt.append("用户: ").append(interaction.getPlayerMessage()).append("\n");
                prompt.append("你: ").append(interaction.getNpcResponse()).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("用户: ").append(playerMessage).append("\n");
        prompt.append("你: ");
        
        return prompt.toString();
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPersonality() { return personality; }
} 