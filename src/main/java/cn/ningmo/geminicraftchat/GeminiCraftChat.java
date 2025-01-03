package cn.ningmo.geminicraftchat;

import cn.ningmo.geminicraftchat.api.GeminiAPI;
import cn.ningmo.geminicraftchat.chat.ChatHistory;
import cn.ningmo.geminicraftchat.chat.ChatListener;
import cn.ningmo.geminicraftchat.chat.RateLimiter;
import cn.ningmo.geminicraftchat.command.GCCCommand;
import cn.ningmo.geminicraftchat.log.ChatLogger;
import cn.ningmo.geminicraftchat.log.DebugLogger;
import cn.ningmo.geminicraftchat.npc.NPCManager;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import cn.ningmo.geminicraftchat.filter.WordFilter;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class GeminiCraftChat extends JavaPlugin {
    private GeminiAPI geminiAPI;
    private ChatHistory chatHistory;
    private RateLimiter rateLimiter;
    private PersonaManager personaManager;
    private NPCManager npcManager;
    private DebugLogger debugLogger;
    private ChatLogger chatLogger;
    private WordFilter wordFilter;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化组件
        initComponents();
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(
            new ChatListener(this), this);
            
        // 注册命令
        getCommand("gcc").setExecutor(new GCCCommand(this));
        
        getLogger().info("GeminiCraftChat 已启用！");
    }
    
    private void initComponents() {
        this.debugLogger = new DebugLogger(this);
        this.chatLogger = new ChatLogger(this);
        this.wordFilter = new WordFilter(this);
        this.geminiAPI = new GeminiAPI(this);
        this.chatHistory = new ChatHistory(this);
        this.rateLimiter = new RateLimiter(this);
        this.personaManager = new PersonaManager(this);
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
} 