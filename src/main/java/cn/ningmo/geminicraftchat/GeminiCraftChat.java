package cn.ningmo.geminicraftchat;

import cn.ningmo.geminicraftchat.api.GeminiAPI;
import cn.ningmo.geminicraftchat.chat.ChatHistory;
import cn.ningmo.geminicraftchat.command.GCCCommand;
import cn.ningmo.geminicraftchat.command.GCCTabCompleter;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import cn.ningmo.geminicraftchat.platform.Platform;
import cn.ningmo.geminicraftchat.platform.impl.BukkitPlatform;
import cn.ningmo.geminicraftchat.util.RateLimiter;
import cn.ningmo.geminicraftchat.log.ChatLogger;
import cn.ningmo.geminicraftchat.npc.NPCManager;
import cn.ningmo.geminicraftchat.filter.WordFilter;
import org.bukkit.plugin.java.JavaPlugin;

public class GeminiCraftChat extends JavaPlugin {
    private GeminiAPI geminiAPI;
    private PersonaManager personaManager;
    private RateLimiter rateLimiter;
    private ChatHistory chatHistory;
    private Platform platform;
    private ChatLogger chatLogger;
    private NPCManager npcManager;
    private WordFilter wordFilter;
    private DebugLogger debugLogger;
    
    @Override
    public void onEnable() {
        // 初始化平台
        this.platform = new BukkitPlatform(this);
        
        // 加载配置文件
        saveDefaultConfig();
        
        // 初始化组件
        this.geminiAPI = new GeminiAPI(this);
        this.personaManager = new PersonaManager(this);
        this.rateLimiter = new RateLimiter(getConfig().getLong("chat.cooldown"));
        this.chatHistory = new ChatHistory(
            this,
            getConfig().getInt("chat.max_history"),
            getConfig().getBoolean("chat.independent_chat", true)
        );
        this.chatLogger = new ChatLogger(this);
        this.npcManager = new NPCManager(this);
        this.wordFilter = new WordFilter(this);
        this.debugLogger = new DebugLogger(this);
        
        // 注册命令
        getCommand("gcc").setExecutor(new GCCCommand(this));
        getCommand("gcc").setTabCompleter(new GCCTabCompleter(this));
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        getLogger().info("GeminiCraftChat 已启动! 平台: " + platform.getType());
    }
    
    @Override
    public void onDisable() {
        getLogger().info("GeminiCraftChat 已关闭!");
    }
    
    public Platform getPlatform() {
        return platform;
    }
    
    public GeminiAPI getGeminiAPI() {
        return geminiAPI;
    }
    
    public PersonaManager getPersonaManager() {
        return personaManager;
    }
    
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    public ChatHistory getChatHistory() {
        return chatHistory;
    }
    
    public ChatLogger getChatLogger() {
        return chatLogger;
    }
    
    public NPCManager getNPCManager() {
        return npcManager;
    }
    
    public WordFilter getWordFilter() {
        return wordFilter;
    }
    
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }
} 