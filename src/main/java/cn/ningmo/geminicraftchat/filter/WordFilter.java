package cn.ningmo.geminicraftchat.filter;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.Configuration;

import java.util.*;

public class WordFilter {
    private final GeminiCraftChat plugin;
    private final Map<Character, Map<?, ?>> wordTree;
    private boolean enabled;
    private String replacement;
    
    public WordFilter(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.wordTree = new HashMap<>();
        loadConfig();
    }
    
    public void loadConfig() {
        Configuration config = plugin.getConfig();
        this.enabled = config.getBoolean("word_filter.enabled", true);
        this.replacement = config.getString("word_filter.replacement", "***");
        
        // 清空现有树
        wordTree.clear();
        
        // 加载敏感词
        List<String> words = config.getStringList("word_filter.words");
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            addWord(word);
        }
    }
    
    private void addWord(String word) {
        Map<Object, Object> currentMap = (Map<Object, Object>) wordTree;
        
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            
            // 获取当前字符的子节点
            Map<Object, Object> subMap = (Map<Object, Object>) currentMap.get(c);
            
            if (subMap == null) {
                // 如果子节点不存在，创建新的子节点
                subMap = new HashMap<>();
                currentMap.put(c, subMap);
            }
            
            // 移动到下一个节点
            currentMap = subMap;
            
            // 如果是最后一个字符，标记为敏感词结尾
            if (i == word.length() - 1) {
                currentMap.put("isEnd", true);
            }
        }
    }
    
    public String filter(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder(text);
        Map<Object, Object> currentMap;
        int begin = 0;
        int position = 0;
        
        while (position < text.length()) {
            char c = text.charAt(position);
            currentMap = (Map<Object, Object>) wordTree.get(c);
            
            if (currentMap == null) {
                position++;
                begin = position;
                continue;
            }
            
            // 检查是否是敏感词结尾
            if (currentMap.containsKey("isEnd")) {
                // 替换敏感词
                int length = position - begin + 1;
                result.replace(begin, begin + length, replacement);
                
                // 移动指针
                position++;
                begin = position;
            } else {
                // 继续检查下一个字符
                position++;
            }
        }
        
        return result.toString();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("word_filter.enabled", enabled);
        plugin.saveConfig();
    }
    
    public void addFilterWord(String word) {
        if (word == null || word.isEmpty()) return;
        
        // 添加到树中
        addWord(word);
        
        // 保存到配置文件
        List<String> words = plugin.getConfig().getStringList("word_filter.words");
        if (!words.contains(word)) {
            words.add(word);
            plugin.getConfig().set("word_filter.words", words);
            plugin.saveConfig();
        }
    }
    
    public void removeFilterWord(String word) {
        if (word == null || word.isEmpty()) return;
        
        // 从配置文件中移除
        List<String> words = plugin.getConfig().getStringList("word_filter.words");
        if (words.remove(word)) {
            plugin.getConfig().set("word_filter.words", words);
            plugin.saveConfig();
            
            // 重新构建树
            loadConfig();
        }
    }
} 