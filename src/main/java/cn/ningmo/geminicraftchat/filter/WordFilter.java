package cn.ningmo.geminicraftchat.filter;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class WordFilter {
    private final GeminiCraftChat plugin;
    private final Map<Character, Map<Character, Object>> wordTree;
    private boolean enabled;
    private String replacement;

    public WordFilter(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.wordTree = new HashMap<>();
        reload();
    }

    public void reload() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("word_filter");
        if (config == null) {
            this.enabled = false;
            this.replacement = "***";
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.replacement = config.getString("replacement", "***");

        // 重建敏感词树
        wordTree.clear();
        List<String> words = config.getStringList("words");
        for (String word : words) {
            addWord(word);
        }
    }

    private void addWord(String word) {
        Map<Character, Object> currentMap = wordTree;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Map<Character, Object> nextMap = (Map<Character, Object>) currentMap.get(c);
            if (nextMap == null) {
                nextMap = new HashMap<>();
                currentMap.put(c, nextMap);
            }
            currentMap = nextMap;
        }
        currentMap.put('\0', null); // 结束标记
    }

    public String filter(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            int length = checkWord(text, i);
            if (length > 0) {
                for (int j = 0; j < length; j++) {
                    result.setCharAt(i + j, replacement.charAt(Math.min(j, replacement.length() - 1)));
                }
                i += length - 1;
            }
        }
        return result.toString();
    }

    private int checkWord(String text, int start) {
        Map<Character, Object> currentMap = wordTree;
        int maxLength = 0;
        int position = start;

        while (position < text.length()) {
            Map<Character, Object> nextMap = (Map<Character, Object>) currentMap.get(text.charAt(position));
            if (nextMap == null) {
                break;
            }
            currentMap = nextMap;
            position++;
            if (currentMap.containsKey('\0')) {
                maxLength = position - start;
            }
        }

        return maxLength;
    }

    public void addWord(String word, boolean save) {
        addWord(word);
        if (save) {
            List<String> words = new ArrayList<>(plugin.getConfig().getStringList("word_filter.words"));
            if (!words.contains(word)) {
                words.add(word);
                plugin.getConfig().set("word_filter.words", words);
                plugin.saveConfig();
            }
        }
    }

    public void removeWord(String word) {
        List<String> words = new ArrayList<>(plugin.getConfig().getStringList("word_filter.words"));
        if (words.remove(word)) {
            plugin.getConfig().set("word_filter.words", words);
            plugin.saveConfig();
            reload(); // 重新构建敏感词树
        }
    }

    public List<String> getWords() {
        return plugin.getConfig().getStringList("word_filter.words");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("word_filter.enabled", enabled);
        plugin.saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }
} 