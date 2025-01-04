package cn.ningmo.geminicraftchat;

import cn.ningmo.geminicraftchat.api.GeminiAPI;
import cn.ningmo.geminicraftchat.chat.ChatHistory;
import cn.ningmo.geminicraftchat.chat.ChatListener;
import cn.ningmo.geminicraftchat.chat.RateLimiter;
import cn.ningmo.geminicraftchat.command.GCCCommand;
import cn.ningmo.geminicraftchat.command.GCCTabCompleter;
import cn.ningmo.geminicraftchat.log.ChatLogger;
import cn.ningmo.geminicraftchat.log.DebugLogger;
import cn.ningmo.geminicraftchat.npc.NPCManager;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import cn.ningmo.geminicraftchat.filter.WordFilter;
import cn.ningmo.geminicraftchat.platform.Platform;
import cn.ningmo.geminicraftchat.platform.impl.BukkitPlatform;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class GeminiCraftChat extends JavaPlugin {
    private GeminiAPI geminiAPI;
    private ChatHistory chatHistory;
    private RateLimiter rateLimiter;
    private PersonaManager personaManager;
    private NPCManager npcManager;
    private DebugLogger debugLogger;
    private ChatLogger chatLogger;
    private WordFilter wordFilter;
    private Platform platform;

    @Override
    public void onEnable() {
        getLogger().info("正在启动 GeminiCraftChat...");
        
        // 保存默认配置
        saveDefaultConfig();
        getLogger().info("配置文件已加载");
        
        // 初始化组件
        try {
            initComponents();
            getLogger().info("组件初始化完成");
        } catch (Exception e) {
            getLogger().severe("组件初始化失败：" + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 注册事件监听器
        try {
            getServer().getPluginManager().registerEvents(
                new ChatListener(this), this);
            getLogger().info("事件监听器注册完成");
        } catch (Exception e) {
            getLogger().severe("事件监听器注册失败：" + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
            
        // 注册命令
        try {
            GCCCommand commandExecutor = new GCCCommand(this);
            GCCTabCompleter tabCompleter = new GCCTabCompleter(this);
            
            getCommand("gcc").setExecutor(commandExecutor);
            getCommand("gcc").setTabCompleter(tabCompleter);
            getLogger().info("命令注册完成");
        } catch (Exception e) {
            getLogger().severe("命令注册失败：" + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("=================================");
        getLogger().info("    GeminiCraftChat 已启用！");
        getLogger().info("    版本: " + getDescription().getVersion());
        getLogger().info("    作者: " + getDescription().getAuthors().get(0));
        getLogger().info("=================================");
    }
    
    private void initComponents() {
        getLogger().info("正在初始化平台适配器...");
        this.platform = new BukkitPlatform(this);
        
        getLogger().info("正在初始化日志系统...");
        this.debugLogger = new DebugLogger(this);
        this.chatLogger = new ChatLogger(this);
        
        getLogger().info("正在初始化过滤系统...");
        this.wordFilter = new WordFilter(this);
        
        getLogger().info("正在初始化 API 客户端...");
        this.geminiAPI = new GeminiAPI(this);
        
        getLogger().info("正在初始化聊天系统...");
        this.chatHistory = new ChatHistory(this);
        this.rateLimiter = new RateLimiter(this);
        
        getLogger().info("正在初始化人设系统...");
        this.personaManager = new PersonaManager(this);
        
        getLogger().info("正在初始化 NPC 系统...");
        this.npcManager = new NPCManager(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("GeminiCraftChat 已禁用！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gcreload")) {
            if (!sender.hasPermission("gcc.admin")) {
                sender.sendMessage("§c你没有权限执行此命令！");
                return true;
            }

            try {
                reload();
                sender.sendMessage("§a配置已重新加载！");
            } catch (Exception e) {
                sender.sendMessage("§c重载失败：" + e.getMessage());
                getLogger().severe("重载失败：" + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    /**
     * 重新加载插件
     */
    public void reload() {
        // 重新加载配置文件
        reloadConfig();
        
        // 重新加载各个组件
        debugLogger.loadConfig();
        chatLogger.reload();
        wordFilter.reload();
        geminiAPI.reload();
        chatHistory.reload();
        rateLimiter.reload();
        personaManager.reload();
        npcManager.reload();
        
        getLogger().info("插件重载完成！");
    }

    // Getters
    public GeminiAPI getGeminiAPI() { return geminiAPI; }
    public ChatHistory getChatHistory() { return chatHistory; }
    public RateLimiter getRateLimiter() { return rateLimiter; }
    public PersonaManager getPersonaManager() { return personaManager; }
    public NPCManager getNpcManager() { return npcManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public ChatLogger getChatLogger() { return chatLogger; }
    public WordFilter getWordFilter() { return wordFilter; }
    public Platform getPlatform() { return platform; }
} 