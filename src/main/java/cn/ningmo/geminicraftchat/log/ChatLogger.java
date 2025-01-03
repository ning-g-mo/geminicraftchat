package cn.ningmo.geminicraftchat.log;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.ConfigurationSection;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatLogger {
    private final GeminiCraftChat plugin;
    private final File logFolder;
    private final SimpleDateFormat dateFormat;
    private boolean enabled;
    private String logFormat;

    public ChatLogger(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        reload();
        
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }

    public void reload() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("chat_log");
        if (config == null) {
            this.enabled = false;
            this.logFormat = "[%time%] %player%: %message% -> %response%";
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.logFormat = config.getString("format", "[%time%] %player%: %message% -> %response%");
    }

    public void logChat(String playerName, String message, String response) {
        if (!enabled) return;

        String fileName = String.format("chat-%s.log", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        File logFile = new File(logFolder, fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String logEntry = logFormat
                .replace("%time%", dateFormat.format(new Date()))
                .replace("%player%", playerName)
                .replace("%message%", message)
                .replace("%response%", response);
            writer.println(logEntry);
        } catch (IOException e) {
            plugin.getLogger().warning("无法写入聊天日志: " + e.getMessage());
        }
    }
} 