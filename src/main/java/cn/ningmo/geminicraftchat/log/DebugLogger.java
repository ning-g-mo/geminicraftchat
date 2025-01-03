package cn.ningmo.geminicraftchat.log;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.Configuration;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

public class DebugLogger {
    private final GeminiCraftChat plugin;
    private final File debugFolder;
    private final SimpleDateFormat dateFormat;
    private boolean enabled;
    private boolean logApiCalls;
    private boolean logChatHistory;
    private Level logLevel;
    private int maxFileSize;
    private int maxHistory;
    private boolean compressLogs;
    private boolean logSlowApiCalls;
    private long slowApiThreshold;
    private boolean logMemoryUsage;
    private int memoryCheckInterval;
    private boolean logStackTrace;
    private boolean logErrorContext;
    private boolean notifyAdmin;
    private boolean logConfigChanges;
    private boolean logCommandExecution;
    private boolean logPermissionChecks;
    
    public DebugLogger(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.debugFolder = new File(plugin.getDataFolder(), "debug");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        loadConfig();
        
        if (!debugFolder.exists()) {
            debugFolder.mkdirs();
        }
        
        // 启动内存监控
        if (enabled && logMemoryUsage) {
            startMemoryMonitoring();
        }
    }
    
    public void loadConfig() {
        Configuration config = plugin.getConfig();
        this.enabled = config.getBoolean("debug.enabled", false);
        this.logApiCalls = config.getBoolean("debug.log_api_calls", false);
        this.logChatHistory = config.getBoolean("debug.log_chat_history", false);
        this.logLevel = Level.parse(config.getString("debug.log_level", "INFO"));
        
        // 文件设置
        this.maxFileSize = config.getInt("debug.file.max_size", 10) * 1024 * 1024; // 转换为字节
        this.maxHistory = config.getInt("debug.file.max_history", 7);
        this.compressLogs = config.getBoolean("debug.file.compress", true);
        
        // 性能监控
        this.logSlowApiCalls = config.getBoolean("debug.performance.log_slow_api_calls", true);
        this.slowApiThreshold = config.getLong("debug.performance.slow_api_threshold", 1000);
        this.logMemoryUsage = config.getBoolean("debug.performance.log_memory_usage", false);
        this.memoryCheckInterval = config.getInt("debug.performance.memory_check_interval", 300);
        
        // 错误追踪
        this.logStackTrace = config.getBoolean("debug.error_tracking.log_stack_trace", true);
        this.logErrorContext = config.getBoolean("debug.error_tracking.log_error_context", true);
        this.notifyAdmin = config.getBoolean("debug.error_tracking.notify_admin", true);
        
        // 开发者选项
        this.logConfigChanges = config.getBoolean("debug.developer.log_config_changes", true);
        this.logCommandExecution = config.getBoolean("debug.developer.log_command_execution", true);
        this.logPermissionChecks = config.getBoolean("debug.developer.log_permission_checks", false);
    }
    
    public void logApiCall(String endpoint, String request, String response, long duration) {
        if (!enabled || !logApiCalls) return;
        
        boolean isSlowCall = duration > slowApiThreshold;
        if (!logApiCalls && !(logSlowApiCalls && isSlowCall)) return;
        
        String fileName = String.format("api-%s.log", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        File logFile = new File(debugFolder, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.printf("[%s] %sAPI调用:\n", timestamp, isSlowCall ? "慢速" : "");
            writer.printf("端点: %s\n", endpoint);
            writer.printf("请求内容:\n%s\n", request);
            writer.printf("响应内容:\n%s\n", response);
            writer.printf("耗时: %dms\n", duration);
            writer.println("----------------------------------------");
            
            checkFileSize(logFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法写入API调试日志", e);
        }
    }
    
    public void logChatHistory(String playerId, String context) {
        if (!enabled || !logChatHistory) return;
        
        String fileName = String.format("chat-%s.log", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        File logFile = new File(debugFolder, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.printf("[%s] 聊天历史记录 (%s):\n", timestamp, playerId);
            writer.println(context);
            writer.println("----------------------------------------");
            
            checkFileSize(logFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法写入聊天历史调试日志", e);
        }
    }
    
    public void logDebug(String message) {
        if (!enabled || logLevel.intValue() > Level.INFO.intValue()) return;
        
        String fileName = String.format("debug-%s.log", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        File logFile = new File(debugFolder, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.printf("[%s] %s\n", timestamp, message);
            
            checkFileSize(logFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法写入调试日志", e);
        }
    }
    
    public void logDebug(String message, Throwable error) {
        if (!enabled) return;
        
        String fileName = String.format("debug-%s.log", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        File logFile = new File(debugFolder, fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dateFormat.format(new Date());
            writer.printf("[%s] %s\n", timestamp, message);
            
            if (logStackTrace) {
                error.printStackTrace(writer);
            } else {
                writer.println("错误: " + error.getMessage());
            }
            
            if (logErrorContext) {
                writer.println("错误上下文:");
                // TODO: 添加更多上下文信息
            }
            
            writer.println("----------------------------------------");
            
            checkFileSize(logFile);
            
            if (notifyAdmin) {
                notifyAdmins(message, error);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "无法写入调试日志", e);
        }
    }
    
    private void checkFileSize(File file) throws IOException {
        if (file.length() > maxFileSize) {
            String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
            File archive = new File(debugFolder, baseName + "-" + new Date().getTime() + ".log");
            
            if (compressLogs) {
                try (FileInputStream fis = new FileInputStream(file);
                     FileOutputStream fos = new FileOutputStream(archive + ".gz");
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        gzos.write(buffer, 0, len);
                    }
                }
                archive.delete();
            } else {
                file.renameTo(archive);
            }
            
            file.delete();
            file.createNewFile();
            
            cleanOldLogs();
        }
    }
    
    private void cleanOldLogs() {
        File[] files = debugFolder.listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".log.gz"));
        if (files == null) return;
        
        long cutoffTime = System.currentTimeMillis() - (maxHistory * 24L * 60L * 60L * 1000L);
        for (File file : files) {
            if (file.lastModified() < cutoffTime) {
                file.delete();
            }
        }
    }
    
    private void startMemoryMonitoring() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            logDebug(String.format("内存使用情况 - 已用: %dMB, 空闲: %dMB, 总计: %dMB",
                usedMemory / 1024 / 1024,
                freeMemory / 1024 / 1024,
                totalMemory / 1024 / 1024));
        }, 20L * memoryCheckInterval, 20L * memoryCheckInterval);
    }
    
    private void notifyAdmins(String message, Throwable error) {
        plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("gcc.admin"))
            .forEach(player -> {
                player.sendMessage("§c[GCC调试] 发生错误: " + message);
                player.sendMessage("§c原因: " + error.getMessage());
            });
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isLogApiCalls() { return logApiCalls; }
    public boolean isLogChatHistory() { return logChatHistory; }
    public Level getLogLevel() { return logLevel; }
    public boolean isLogSlowApiCalls() { return logSlowApiCalls; }
    public long getSlowApiThreshold() { return slowApiThreshold; }
    public boolean isLogMemoryUsage() { return logMemoryUsage; }
    public boolean isLogStackTrace() { return logStackTrace; }
    public boolean isLogErrorContext() { return logErrorContext; }
    public boolean isNotifyAdmin() { return notifyAdmin; }
    public boolean isLogConfigChanges() { return logConfigChanges; }
    public boolean isLogCommandExecution() { return logCommandExecution; }
    public boolean isLogPermissionChecks() { return logPermissionChecks; }
} 