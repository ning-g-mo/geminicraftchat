package cn.ningmo.geminicraftchat.command;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Set;

public class GCCCommand implements CommandExecutor {
    private final GeminiCraftChat plugin;
    
    public GCCCommand(GeminiCraftChat plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(player);
                break;
                
            case "reload":
                if (player.hasPermission("gcc.admin")) {
                    plugin.reloadConfig();
                    player.sendMessage("§a配置已重新加载！");
                } else {
                    player.sendMessage("§c你没有权限执行此命令！");
                }
                break;
                
            case "persona":
                handlePersonaCommand(player, args);
                break;
                
            case "model":
                handleModelCommand(player, args);
                break;
                
            case "clear":
                handleClearCommand(player, args);
                break;
                
            case "filter":
                handleFilterCommand(player, args);
                break;
                
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(new String[]{
            "§6=== GeminiCraftChat 帮助 ===",
            "§e/gcc help §7- 显示此帮助",
            "§e/gcc reload §7- 重新加载配置",
            "§e/gcc persona <list|set|create|delete> §7- 人设管理",
            "§e/gcc model <list|set> §7- 模型管理",
            "§e/gcc clear [all] §7- 清除聊天历史",
            "§e/gcc filter <add|remove|list|enable|disable> §7- 敏感词管理"
        });
    }
    
    private void handlePersonaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /gcc persona <list|set|create|delete>");
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "list":
                Set<String> personas = plugin.getConfig().getConfigurationSection("personas").getKeys(false);
                player.sendMessage("§6=== 可用人设列表 ===");
                for (String name : personas) {
                    String description = plugin.getConfig().getString("personas." + name + ".description", "无描述");
                    player.sendMessage(String.format("§e%s §7- %s", name, description));
                }
                break;
                
            case "set":
                if (args.length < 3) {
                    player.sendMessage("§c用法: /gcc persona set <人设名称>");
                    return;
                }
                if (plugin.getPersonaManager().personaExists(args[2])) {
                    plugin.getPersonaManager().setPersona(player, args[2]);
                    player.sendMessage("§a已切换到人设：" + args[2]);
                } else {
                    player.sendMessage("§c找不到指定的人设！");
                }
                break;
                
            case "create":
                // TODO: 创建人设
                break;
                
            case "delete":
                // TODO: 删除人设
                break;
                
            default:
                player.sendMessage("§c未知的子命令！");
                break;
        }
    }
    
    private void handleModelCommand(Player player, String[] args) {
        if (!player.hasPermission("gcc.admin")) {
            player.sendMessage("§c你没有权限执行此命令！");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§c用法: /gcc model <list|set>");
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "list":
                player.sendMessage(new String[]{
                    "§6可用的模型：",
                    "§7- gemini-pro",
                    "§7- gemini-pro-vision"
                });
                break;
                
            case "set":
                if (args.length < 3) {
                    player.sendMessage("§c用法: /gcc model set <模型名称>");
                    return;
                }
                plugin.getGeminiAPI().setModel(args[2]);
                player.sendMessage("§a已切换到模型：" + args[2]);
                break;
                
            default:
                player.sendMessage("§c未知的子命令！");
                break;
        }
    }
    
    private void handleClearCommand(Player player, String[] args) {
        if (args.length > 1 && args[1].equals("all")) {
            if (player.hasPermission("gcc.admin")) {
                plugin.getChatHistory().clearAllHistory();
                player.sendMessage("§a已清除所有玩家的聊天历史！");
            } else {
                player.sendMessage("§c你没有权限执行此命令！");
            }
        } else {
            plugin.getChatHistory().clearHistory(player);
            player.sendMessage("§a已清除你的聊天历史！");
        }
    }
    
    private void handleFilterCommand(Player player, String[] args) {
        if (!player.hasPermission("gcc.admin")) {
            player.sendMessage("§c你没有权限执行此命令！");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§c用法: /gcc filter <add|remove|list|enable|disable>");
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "add":
                if (args.length < 3) {
                    player.sendMessage("§c用法: /gcc filter add <敏感词>");
                    return;
                }
                plugin.getWordFilter().addFilterWord(args[2]);
                player.sendMessage("§a已添加敏感词：" + args[2]);
                break;
                
            case "remove":
                if (args.length < 3) {
                    player.sendMessage("§c用法: /gcc filter remove <敏感词>");
                    return;
                }
                plugin.getWordFilter().removeFilterWord(args[2]);
                player.sendMessage("§a已移除敏感词：" + args[2]);
                break;
                
            case "list":
                player.sendMessage("§6=== 敏感词列表 ===");
                for (String word : plugin.getWordFilter().getWords()) {
                    player.sendMessage("§7- " + word);
                }
                break;
                
            case "enable":
                plugin.getWordFilter().setEnabled(true);
                player.sendMessage("§a已启用敏感词过滤！");
                break;
                
            case "disable":
                plugin.getWordFilter().setEnabled(false);
                player.sendMessage("§a已禁用敏感词过滤！");
                break;
                
            default:
                player.sendMessage("§c未知的子命令！");
                break;
        }
    }
}