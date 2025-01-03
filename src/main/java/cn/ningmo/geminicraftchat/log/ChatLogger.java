package cn.ningmo.geminicraftchat.log;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class ChatLogger {
    private final GeminiCraftChat plugin;
    private final File logFolder;
    private final SimpleDateFormat dateFormat;
    
    public ChatLogger(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }
    
    public void logChat(Player player, String message, String response, boolean success) {
        String fileName = String.format("%s.log", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        File logFile = new File(logFolder, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.printf("[%s] %s (%s):\n", timestamp, player.getName(), player.getUniqueId());
            writer.printf("Message: %s\n", message);
            writer.printf("Success: %s\n", success);
            writer.printf("Response: %s\n", response);
            writer.println("----------------------------------------");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法写入聊天日志", e);
        }
    }
    
    public void logError(Player player, String message, Throwable error) {
        String fileName = String.format("errors-%s.log", new SimpleDateFormat("yyyy-MM").format(new Date()));
        File logFile = new File(logFolder, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.printf("[%s] %s (%s):\n", timestamp, player.getName(), player.getUniqueId());
            writer.printf("Message: %s\n", message);
            writer.println("Error: ");
            error.printStackTrace(writer);
            writer.println("----------------------------------------");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法写入错误日志", e);
        }
    }
} 