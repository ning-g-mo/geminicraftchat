package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.api.GeminiAPI;
import cn.ningmo.geminicraftchat.filter.WordFilter;
import cn.ningmo.geminicraftchat.log.ChatLogger;
import cn.ningmo.geminicraftchat.npc.NPC;
import cn.ningmo.geminicraftchat.npc.NPCManager;
import cn.ningmo.geminicraftchat.persona.Persona;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import cn.ningmo.geminicraftchat.response.GeminiResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Optional;

public class ChatListener implements Listener {
    private final GeminiCraftChat plugin;
    private final GeminiAPI geminiAPI;
    private final ChatHistory chatHistory;
    private final RateLimiter rateLimiter;
    private final PersonaManager personaManager;
    private final NPCManager npcManager;
    private final ChatLogger chatLogger;
    private final WordFilter wordFilter;
    private final String triggerWord;

    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.geminiAPI = plugin.getGeminiAPI();
        this.chatHistory = plugin.getChatHistory();
        this.rateLimiter = plugin.getRateLimiter();
        this.personaManager = plugin.getPersonaManager();
        this.npcManager = plugin.getNpcManager();
        this.chatLogger = plugin.getChatLogger();
        this.wordFilter = plugin.getWordFilter();
        this.triggerWord = plugin.getConfig().getString("chat.trigger", "ai");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 检查是否是AI触发消息
        if (!shouldTriggerAI(message)) {
            // 检查是否触发NPC响应
            Optional<NPC> npc = npcManager.tryTriggerNPC(message);
            if (npc.isPresent()) {
                handleNPCResponse(player, message, npc.get());
            }
            return;
        }

        // 移除触发词
        message = removeTriggerWord(message);

        // 检查速率限制
        if (!rateLimiter.tryAcquire(player)) {
            long remainingCooldown = rateLimiter.getRemainingCooldown(player);
            player.sendMessage("§c请等待 " + (remainingCooldown / 1000) + " 秒后再发送消息。");
            event.setCancelled(true);
            return;
        }

        // 过滤敏感词
        String filteredMessage = wordFilter.filter(message);
        if (!filteredMessage.equals(message)) {
            player.sendMessage("§c你的消息包含敏感词，已被过滤。");
            message = filteredMessage;
        }

        // 获取玩家的人设
        Persona persona = personaManager.getPersona(player);
        String prompt = persona != null ? persona.getPrompt() : "";

        // 获取聊天历史
        String history = String.join("\n", chatHistory.getHistory(player));

        // 构建完整的消息
        String fullMessage = (prompt.isEmpty() ? "" : prompt + "\n") +
                           (history.isEmpty() ? "" : "历史消息:\n" + history + "\n") +
                           "用户: " + message;

        // 发送思考中消息
        String thinkingMessage = plugin.getConfig().getString("chat.format.thinking", "§7[AI] §f正在思考中...");
        player.sendMessage(thinkingMessage);

        // 异步处理AI响应
        final String finalMessage = message;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 记录玩家消息
                chatHistory.addMessage(player, "用户: " + finalMessage);

                // 调用API获取响应
                GeminiResponse response = geminiAPI.chat(fullMessage);
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getError());
                }

                String aiResponse = response.getMessage();
                chatHistory.addMessage(player, "AI: " + aiResponse);

                // 记录聊天日志
                chatLogger.logChat(player, finalMessage, aiResponse, false);

                // 发送响应给玩家
                String responseFormat = plugin.getConfig().getString("chat.format.response", "§7[AI] §f%s");
                String formattedResponse = String.format(responseFormat, aiResponse);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(formattedResponse);
                });

            } catch (RuntimeException e) {
                chatLogger.logError(player, finalMessage, e);
                String errorFormat = plugin.getConfig().getString("chat.format.error", "§c[AI] 发生错误：%s");
                String errorMessage = String.format(errorFormat, e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(errorMessage);
                });
            }
        });

        // 取消原始聊天消息
        event.setCancelled(true);
    }

    private boolean shouldTriggerAI(String message) {
        return message.toLowerCase().startsWith(triggerWord.toLowerCase() + " ") || 
               message.toLowerCase().equals(triggerWord.toLowerCase());
    }

    private String removeTriggerWord(String message) {
        if (message.toLowerCase().equals(triggerWord.toLowerCase())) {
            return "";
        }
        return message.substring(triggerWord.length()).trim();
    }

    private void handleNPCResponse(Player player, String message, NPC npc) {
        // 发送思考中消息
        String thinkingMessage = plugin.getConfig().getString("chat.format.thinking", "§7[" + npc.getName() + "] §f正在思考中...");
        player.sendMessage(thinkingMessage);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 获取NPC的历史记录
                String prompt = npc.getPrompt(message, npcManager.getMemory(npc.getId()));

                // 调用API获取响应
                GeminiResponse response = geminiAPI.chat(prompt);
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getError());
                }

                String npcResponse = response.getMessage();
                npcManager.addMemory(npc.getId(), message, npcResponse);

                // 记录聊天日志
                chatLogger.logChat(player, message, npcResponse, true);

                // 发送响应给玩家
                String responseFormat = plugin.getConfig().getString("chat.format.npc_response", "§7[%s] §f%s");
                String formattedResponse = String.format(responseFormat, npc.getName(), npcResponse);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(formattedResponse);
                });

            } catch (RuntimeException e) {
                chatLogger.logError(player, message, e);
                String errorFormat = plugin.getConfig().getString("chat.format.error", "§c[%s] 发生错误：%s");
                String errorMessage = String.format(errorFormat, npc.getName(), e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(errorMessage);
                });
            }
        });
    }
}