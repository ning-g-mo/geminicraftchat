package cn.ningmo.geminicraftchat.command;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.*;

public class GCCTabCompleter implements TabCompleter {
    private final GeminiCraftChat plugin;
    private final Map<String, List<String>> subCommands;
    
    public GCCTabCompleter(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        initSubCommands();
    }
    
    private void initSubCommands() {
        // 基础命令
        subCommands.put("", Arrays.asList("help", "reload", "persona", "model", "clear", "filter"));
        
        // persona 子命令
        subCommands.put("persona", Arrays.asList("list", "set", "create", "delete"));
        
        // model 子命令
        subCommands.put("model", Arrays.asList("list", "set"));
        
        // clear 子命令
        subCommands.put("clear", Arrays.asList("all", "history"));
        
        // filter 子命令
        subCommands.put("filter", Arrays.asList("add", "remove", "list", "enable", "disable"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("gcc")) {
            if (args.length == 1) {
                // 主命令补全
                StringUtil.copyPartialMatches(args[0], subCommands.get(""), completions);
            } else if (args.length == 2) {
                // 子命令补全
                String subCommand = args[0].toLowerCase();
                List<String> subCommandList = subCommands.get(subCommand);
                
                if (subCommandList != null) {
                    StringUtil.copyPartialMatches(args[1], subCommandList, completions);
                }
                
                // 特殊补全逻辑
                switch (subCommand) {
                    case "persona":
                        if (args[1].equalsIgnoreCase("set")) {
                            // 添加人设列表
                            completions.addAll(getPersonaList());
                        }
                        break;
                    case "model":
                        if (args[1].equalsIgnoreCase("set")) {
                            // 添加可用模型列表
                            completions.addAll(Arrays.asList("gemini-pro", "gemini-pro-vision"));
                        }
                        break;
                    case "filter":
                        if (args[1].equalsIgnoreCase("remove")) {
                            // 添加现有敏感词列表
                            completions.addAll(getFilterWordList());
                        }
                        break;
                }
            } else if (args.length == 3) {
                // 第三级命令补全
                String subCommand = args[0].toLowerCase();
                String action = args[1].toLowerCase();
                
                if (subCommand.equals("persona") && action.equals("set")) {
                    completions.addAll(getPersonaList());
                } else if (subCommand.equals("filter") && action.equals("remove")) {
                    completions.addAll(getFilterWordList());
                }
            }
        }
        
        Collections.sort(completions); // 按字母顺序排序
        return completions;
    }
    
    private List<String> getPersonaList() {
        Set<String> personas = plugin.getConfig().getConfigurationSection("personas").getKeys(false);
        return new ArrayList<>(personas);
    }
    
    private List<String> getFilterWordList() {
        return plugin.getConfig().getStringList("word_filter.words");
    }
} 