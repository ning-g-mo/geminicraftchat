package cn.ningmo.geminicraftchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            "§e/gcc persona <list|set|create> §7- 人设管理",
            "§e/gcc model <list|set> §7- 模型管理",
            "§e/gcc clear §7- 清除你的聊天历史",
            player.hasPermission("gcc.admin") ? "§e/gcc clear all §7- 清除所有玩家的聊天历史" : null
        });
    }
    
    private void handlePersonaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /gcc persona <list|set|create>");
            return;
        }
        // TODO: 实现人设管理逻辑
    }
    
    private void handleModelCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /gcc model <list|set>");
            return;
        }
        // TODO: 实现模型管理逻辑
    }
} 