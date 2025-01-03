package cn.ningmo.geminicraftchat.chat;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.npc.NPC;
import cn.ningmo.geminicraftchat.persona.Persona;
import cn.ningmo.geminicraftchat.response.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final GeminiCraftChat plugin;
    private final String trigger;

    public ChatListener(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.trigger = plugin.getConfig().getString("chat.trigger", "ai");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        // 检查是否是AI触发消息
        if (!message.toLowerCase().startsWith(trigger.toLowerCase() + " ")) {
            // 检查是否触发NPC对话
            Optional<NPC> npc = plugin.getNpcManager().tryTriggerNPC(message);
            if (npc.isPresent()) {
                handleNPCChat(player, message, npc.get());
            }
            return;
        }

        // 检查权限
        if (!player.hasPermission("gcc.use")) {
            player.sendMessage("§c你没有权限使用此功能！");
            return;
        }

        // 检查冷却
        if (!plugin.getRateLimiter().canSendMessage(player)) {
            long remaining = plugin.getRateLimiter().getRemainingCooldown(player);
            player.sendMessage(String.format("§c请等待 %.1f 秒后再试！", remaining / 1000.0));
            return;
        }

        // 获取实际消息内容
        String content = message.substring(trigger.length()).trim();
        if (content.isEmpty()) {
            player.sendMessage("§c请输入要发送的消息！");
            return;
        }

        // 检查消息长度
        int maxLength = plugin.getConfig().getInt("chat.max_length", 500);
        if (content.length() > maxLength) {
            player.sendMessage("§c消息长度不能超过 " + maxLength + " 个字符！");
            return;
        }

        // 敏感词过滤
        if (plugin.getConfig().getBoolean("word_filter.enabled", true)) {
            String filtered = plugin.getWordFilter().filter(content);
            if (!filtered.equals(content)) {
                player.sendMessage("§c消息包含敏感词，已被过滤！");
                content = filtered;
            }
        }

        // 获取当前人设
        String personaId = plugin.getPersonaManager().getPersona(player.getUniqueId().toString());
        Persona persona = plugin.getPersonaManager().getPersona(personaId);
        if (persona == null) {
            player.sendMessage("§c当前人设无效，请重新设置！");
            return;
        }

        // 发送思考消息
        String thinkingFormat = plugin.getConfig().getString("chat.format.thinking", "§7[AI] §f正在思考中...");
        player.sendMessage(thinkingFormat);

        // 调用API
        plugin.getGeminiAPI().chatAsync(content, personaId)
            .thenAccept(response -> {
                String format;
                String finalMessage;

                if (response.isSuccess()) {
                    // 敏感词过滤
                    String filtered = plugin.getWordFilter().filter(response.getMessage());
                    format = plugin.getConfig().getString("chat.format.response", "§7[AI] §f%s");
                    finalMessage = String.format(format, filtered);
                } else {
                    format = plugin.getConfig().getString("chat.format.error", "§c[AI] 发生错误：%s");
                    finalMessage = String.format(format, response.getError());
                }

                // 添加到历史记录
                plugin.getChatHistory().addMessage(player, content);

                // 记录聊天日志
                plugin.getChatLogger().logChat(
                    player.getName(),
                    content,
                    response.isSuccess() ? response.getMessage() : response.getError()
                );

                // 发送消息
                player.sendMessage(finalMessage);
            })
            .exceptionally(e -> {
                String format = plugin.getConfig().getString("chat.format.error", "§c[AI] 发生错误：%s");
                player.sendMessage(String.format(format, e.getMessage()));
                return null;
            });
    }

    private void handleNPCChat(Player player, String message, NPC npc) {
        plugin.getNPCManager().generateResponse(player.getUniqueId(), npc, message)
            .thenAccept(response -> {
                String format = plugin.getConfig().getString("chat.format.response", "§7[%s] §f%s");
                player.sendMessage(String.format(format, npc.getName(), response));
            })
            .exceptionally(e -> {
                String format = plugin.getConfig().getString("chat.format.error", "§c[%s] 发生错误：%s");
                player.sendMessage(String.format(format, npc.getName(), e.getMessage()));
                return null;
            });
    }
} 