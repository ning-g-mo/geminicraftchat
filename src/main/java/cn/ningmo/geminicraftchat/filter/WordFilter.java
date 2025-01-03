package cn.ningmo.geminicraftchat.filter;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.Configuration;

import java.util.*;

public class WordFilter {
    private final GeminiCraftChat plugin;
    private final TrieNode root;
    private boolean enabled;
    private String replacement;
    
    private static class TrieNode {
        private final Map<Character, TrieNode> children;
        private boolean isEndOfWord;
        
        public TrieNode() {
            this.children = new HashMap<>();
            this.isEndOfWord = false;
        }
        
        public Map<Character, TrieNode> getChildren() {
            return children;
        }
        
        public boolean isEndOfWord() {
            return isEndOfWord;
        }
        
        public void setEndOfWord(boolean endOfWord) {
            isEndOfWord = endOfWord;
        }
    }
    
    public WordFilter(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.root = new TrieNode();
        loadConfig();
    }
    
    public void loadConfig() {
        Configuration config = plugin.getConfig();
        this.enabled = config.getBoolean("word_filter.enabled", true);
        this.replacement = config.getString("word_filter.replacement", "***");
        
        // 清空现有树
        root.getChildren().clear();
        
        // 加载敏感词
        List<String> words = config.getStringList("word_filter.words");
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            addWord(word);
        }
    }
    
    private void addWord(String word) {
        TrieNode current = root;
        
        for (char c : word.toCharArray()) {
            current.getChildren().putIfAbsent(c, new TrieNode());
            current = current.getChildren().get(c);
        }
        
        current.setEndOfWord(true);
    }
    
    public String filter(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder(text);
        int position = 0;
        
        while (position < text.length()) {
            int matchLength = findMatch(text, position);
            if (matchLength > 0) {
                result.replace(position, position + matchLength, replacement);
                position += matchLength;
            } else {
                position++;
            }
        }
        
        return result.toString();
    }
    
    private int findMatch(String text, int start) {
        TrieNode current = root;
        int maxLength = 0;
        int length = 0;
        
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (!current.getChildren().containsKey(c)) {
                break;
            }
            
            current = current.getChildren().get(c);
            length++;
            
            if (current.isEndOfWord()) {
                maxLength = length;
            }
        }
        
        return maxLength;
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