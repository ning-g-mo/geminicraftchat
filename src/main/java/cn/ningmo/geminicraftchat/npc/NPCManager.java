package cn.ningmo.geminicraftchat.npc;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {
    private final GeminiCraftChat plugin;
    private final Map<String, NPC> npcs;
    private final Map<String, List<String>> npcMemory;
    private boolean enableMemory;
    private int maxMemory;
    private int triggerChance;

    public NPCManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.npcs = new ConcurrentHashMap<>();
        this.npcMemory = new ConcurrentHashMap<>();
        reload();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        npcs.clear();
        npcMemory.clear();

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("npc");
        if (config == null) return;

        this.enableMemory = config.getBoolean("enable_memory", true);
        this.maxMemory = config.getInt("max_memory", 20);
        this.triggerChance = config.getInt("trigger_chance", 15);

        ConfigurationSection npcList = config.getConfigurationSection("list");
        if (npcList != null) {
            for (String key : npcList.getKeys(false)) {
                ConfigurationSection npcConfig = npcList.getConfigurationSection(key);
                if (npcConfig != null) {
                    NPC npc = new NPC(
                        key,
                        npcConfig.getString("name", key),
                        npcConfig.getString("description", ""),
                        npcConfig.getString("personality", ""),
                        npcConfig.getStringList("trigger_words")
                    );
                    npcs.put(key, npc);
                }
            }
        }
    }

    /**
     * 尝试触发NPC响应
     */
    public Optional<NPC> tryTriggerNPC(String message) {
        if (new Random().nextInt(100) >= triggerChance) {
            return Optional.empty();
        }

        for (NPC npc : npcs.values()) {
            for (String trigger : npc.getTriggerWords()) {
                if (message.toLowerCase().contains(trigger.toLowerCase())) {
                    return Optional.of(npc);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * 添加NPC记忆
     */
    public void addMemory(String npcId, String memory) {
        if (!enableMemory) return;

        npcMemory.computeIfAbsent(npcId, k -> new ArrayList<>());
        List<String> memories = npcMemory.get(npcId);
        memories.add(memory);

        while (memories.size() > maxMemory) {
            memories.remove(0);
        }
    }

    /**
     * 获取NPC记忆
     */
    public List<String> getMemory(String npcId) {
        return npcMemory.getOrDefault(npcId, new ArrayList<>());
    }

    /**
     * 清除NPC记忆
     */
    public void clearMemory(String npcId) {
        npcMemory.remove(npcId);
    }

    /**
     * 清除所有NPC记忆
     */
    public void clearAllMemory() {
        npcMemory.clear();
    }

    /**
     * 获取所有NPC
     */
    public Collection<NPC> getAllNPCs() {
        return npcs.values();
    }

    /**
     * 获取指定NPC
     */
    public NPC getNPC(String id) {
        return npcs.get(id);
    }

    /**
     * 创建新NPC
     */
    public void createNPC(String id, String name, String description, String personality, List<String> triggerWords) {
        NPC npc = new NPC(id, name, description, personality, triggerWords);
        npcs.put(id, npc);

        // 保存到配置文件
        ConfigurationSection npcSection = plugin.getConfig()
            .createSection("npc.list." + id);
        npcSection.set("name", name);
        npcSection.set("description", description);
        npcSection.set("personality", personality);
        npcSection.set("trigger_words", triggerWords);
        plugin.saveConfig();
    }

    /**
     * 编辑NPC
     */
    public void editNPC(String id, String name, String description, String personality, List<String> triggerWords) {
        if (!npcs.containsKey(id)) {
            throw new IllegalArgumentException("NPC不存在：" + id);
        }

        NPC npc = new NPC(id, name, description, personality, triggerWords);
        npcs.put(id, npc);

        // 更新配置文件
        ConfigurationSection npcSection = plugin.getConfig()
            .getConfigurationSection("npc.list." + id);
        if (npcSection == null) {
            npcSection = plugin.getConfig().createSection("npc.list." + id);
        }
        npcSection.set("name", name);
        npcSection.set("description", description);
        npcSection.set("personality", personality);
        npcSection.set("trigger_words", triggerWords);
        plugin.saveConfig();
    }

    /**
     * 删除NPC
     */
    public void deleteNPC(String id) {
        if (!npcs.containsKey(id)) {
            throw new IllegalArgumentException("NPC不存在：" + id);
        }

        npcs.remove(id);
        clearMemory(id);

        // 从配置文件中删除
        plugin.getConfig().set("npc.list." + id, null);
        plugin.saveConfig();
    }
} 