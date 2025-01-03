package cn.ningmo.geminicraftchat;

import cn.ningmo.geminicraftchat.api.GeminiResponse;
import cn.ningmo.geminicraftchat.npc.NPC;
import cn.ningmo.geminicraftchat.platform.Platform;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Optional;

public class ChatListener implements Listener {
    private final GeminiCraftChat plugin;
    private final Platform platform;
    
    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.platform = plugin.getPlatform();
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        
        // 检查是否触发伪人
        Optional<NPC> npc = plugin.getNPCManager().tryTriggerNPC(message);
        if (npc.isPresent()) {
            handleNPCResponse(player, message, npc.get());
            return;
        }
        
        // 原有的AI聊天逻辑
        String trigger = plugin.getConfig().getString("chat.trigger");
        if (!message.startsWith(trigger)) {
            return;
        }
        
        event.setCancelled(true);
        
        // 检查冷却时间
        if (!player.hasPermission("gcc.bypass_cooldown")) {
            if (!plugin.getRateLimiter().tryAcquire(player.getUniqueId())) {
                long remaining = plugin.getRateLimiter().getRemainingCooldown(player.getUniqueId()) / 1000;
                player.sendMessage(String.format("§c请等待 %d 秒后再次使用！", remaining));
                return;
            }
        }
        
        String question = message.substring(trigger.length()).trim();
        
        if (question.isEmpty()) {
            player.sendMessage("§c请输入要询问的内容！");
            return;
        }
        
        // 检查消息长度
        int maxLength = plugin.getConfig().getInt("chat.max_length", 500);
        if (question.length() > maxLength) {
            player.sendMessage(String.format("§c消息长度超过限制！最大长度：%d", maxLength));
            return;
        }
        
        // 敏感词过滤
        final String filteredQuestion = plugin.getWordFilter().filter(question);
        if (!filteredQuestion.equals(question)) {
            player.sendMessage("§c你的消息包含敏感词，已被过滤。");
        }
        
        // 获取玩家当前使用的人设
        String persona = plugin.getPersonaManager().getPersona(player);
        
        // 使用平台抽象发送消息
        String thinkingFormat = plugin.getConfig().getString("chat.format.thinking", "§7[AI] §f正在思考中...");
        platform.sendMessage(player.getUniqueId(), thinkingFormat);
        
        // 异步调用API
        plugin.getGeminiAPI().chatAsync(filteredQuestion, persona)
            .thenAccept(response -> {
                platform.runSync(() -> {
                    if (response.isSuccess()) {
                        String aiResponse = response.getMessage();
                        // 对AI响应也进行敏感词过滤
                        final String filteredResponse = plugin.getWordFilter().filter(aiResponse);
                        
                        String responseFormat = plugin.getConfig().getString("chat.format.response", "§7[AI] §f%s");
                        platform.sendMessage(player.getUniqueId(), 
                            String.format(responseFormat, filteredResponse));
                        plugin.getChatHistory().addMessage(
                            player.getUniqueId(), 
                            filteredQuestion, 
                            filteredResponse
                        );
                        // 记录成功的对话
                        plugin.getChatLogger().logChat(player, filteredQuestion, filteredResponse, true);
                    } else {
                        String errorFormat = plugin.getConfig().getString("chat.format.error", "§c[AI] 发生错误：%s");
                        platform.sendMessage(player.getUniqueId(), 
                            String.format(errorFormat, response.getError()));
                        // 记录错误
                        plugin.getChatLogger().logError(player, filteredQuestion, 
                            new RuntimeException(response.getError()));
                    }
                });
            });
    }
    
    private void handleNPCResponse(Player player, String message, NPC npc) {
        // 发送思考消息
        platform.sendMessage(player.getUniqueId(), 
            String.format("§d[%s] §7正在思考...", npc.getName()));
        
        // 生成AI响应
        plugin.getNPCManager().generateResponse(player.getUniqueId(), npc, message)
            .thenAccept(response -> {
                platform.runSync(() -> {
                    platform.sendMessage(player.getUniqueId(), 
                        String.format("§d[%s] §f%s", npc.getName(), response));
                });
            });
    }
} 