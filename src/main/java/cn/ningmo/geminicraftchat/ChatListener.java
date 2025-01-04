package cn.ningmo.geminicraftchat;

import cn.ningmo.geminicraftchat.api.GeminiAPI;
import cn.ningmo.geminicraftchat.chat.ChatHistory;
import cn.ningmo.geminicraftchat.chat.RateLimiter;
import cn.ningmo.geminicraftchat.filter.WordFilter;
import cn.ningmo.geminicraftchat.log.ChatLogger;
import cn.ningmo.geminicraftchat.persona.Persona;
import cn.ningmo.geminicraftchat.persona.PersonaManager;
import cn.ningmo.geminicraftchat.response.GeminiResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final GeminiCraftChat plugin;
    private final GeminiAPI geminiAPI;
    private final ChatHistory chatHistory;
    private final RateLimiter rateLimiter;
    private final PersonaManager personaManager;
    private final ChatLogger chatLogger;
    private final WordFilter wordFilter;

    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.geminiAPI = plugin.getGeminiAPI();
        this.chatHistory = plugin.getChatHistory();
        this.rateLimiter = plugin.getRateLimiter();
        this.personaManager = plugin.getPersonaManager();
        this.chatLogger = plugin.getChatLogger();
        this.wordFilter = plugin.getWordFilter();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

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
            event.setMessage(filteredMessage);
        }

        // 获取玩家的人设
        Persona persona = personaManager.getPersona(player);
        String prompt = persona != null ? persona.getPrompt() : "";

        // 获取聊天历史
        String history = String.join("\n", chatHistory.getHistory(player));

        // 构建完整的消息
        String fullMessage = (prompt.isEmpty() ? "" : prompt + "\n") +
                           (history.isEmpty() ? "" : "Previous messages:\n" + history + "\n") +
                           "Player: " + message;

        // 异步处理AI响应
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 记录玩家消息
                chatHistory.addMessage(player, "Player: " + message);

                // 调用API获取响应
                GeminiResponse response = geminiAPI.chat(fullMessage);
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getError());
                }

                String aiResponse = response.getMessage();
                chatHistory.addMessage(player, "AI: " + aiResponse);

                // 记录聊天日志
                chatLogger.logChat(player, message, aiResponse, false);

                // 发送响应给玩家
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§b[AI] §f" + aiResponse);
                });

            } catch (RuntimeException e) {
                chatLogger.logError(player, message, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c处理消息时出错：" + e.getMessage());
                });
            }
        });
    }
} 