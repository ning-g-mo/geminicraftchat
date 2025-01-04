package cn.ningmo.geminicraftchat.log;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.entity.Player;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatLogger {
    private final GeminiCraftChat plugin;
    private final SimpleDateFormat dateFormat;
    private File logFile;
    private boolean enabled;

    public ChatLogger(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("logging.enabled", true);
        if (enabled) {
            String logPath = plugin.getConfig().getString("logging.file", "chat.log");
            logFile = new File(plugin.getDataFolder(), logPath);
            try {
                if (!logFile.exists()) {
                    logFile.getParentFile().mkdirs();
                    logFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建日志文件: " + e.getMessage());
                enabled = false;
            }
        }
    }

    public void logChat(Player player, String message, String response, boolean isNPC) {
        if (!enabled) return;

        String timestamp = dateFormat.format(new Date());
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        String logEntry = String.format("[%s] %s (%s): %s%n", timestamp, playerName, playerUUID, message);
        if (response != null) {
            logEntry += String.format("[%s] %s: %s%n", timestamp, isNPC ? "NPC" : "AI", response);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getLogger().severe("无法写入日志: " + e.getMessage());
        }
    }

    public void logError(Player player, String message, RuntimeException error) {
        if (!enabled) return;

        String timestamp = dateFormat.format(new Date());
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        String logEntry = String.format("[%s] ERROR - %s (%s): %s%n", timestamp, playerName, playerUUID, message);
        logEntry += String.format("[%s] Exception: %s%n", timestamp, error.getMessage());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
            error.printStackTrace(new PrintWriter(writer));
        } catch (IOException e) {
            plugin.getLogger().severe("无法写入错误日志: " + e.getMessage());
        }
    }
}