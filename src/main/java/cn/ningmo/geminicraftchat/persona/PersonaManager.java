package cn.ningmo.geminicraftchat.persona;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PersonaManager {
    private final GeminiCraftChat plugin;
    private final Map<String, Persona> personas;

    public PersonaManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.personas = new HashMap<>();
        reload();
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        personas.clear();
        ConfigurationSection personasSection = plugin.getConfig().getConfigurationSection("personas");
        if (personasSection != null) {
            for (String key : personasSection.getKeys(false)) {
                ConfigurationSection personaSection = personasSection.getConfigurationSection(key);
                if (personaSection != null) {
                    Persona persona = new Persona(
                        key,
                        personaSection.getString("name", key),
                        personaSection.getString("description", ""),
                        personaSection.getString("context", "")
                    );
                    personas.put(key, persona);
                }
            }
        }
    }

    /**
     * 获取人设
     */
    public Persona getPersona(String key) {
        return personas.get(key);
    }

    /**
     * 获取所有人设
     */
    public Set<String> getPersonaKeys() {
        return personas.keySet();
    }

    /**
     * 创建新人设
     */
    public void createPersona(String key, String name, String description, String context) {
        Persona persona = new Persona(key, name, description, context);
        personas.put(key, persona);
        
        // 保存到配置文件
        ConfigurationSection personaSection = plugin.getConfig()
            .createSection("personas." + key);
        personaSection.set("name", name);
        personaSection.set("description", description);
        personaSection.set("context", context);
        plugin.saveConfig();
    }

    /**
     * 编辑人设
     */
    public void editPersona(String key, String name, String description, String context) {
        if (!personas.containsKey(key)) {
            throw new IllegalArgumentException("人设不存在：" + key);
        }

        Persona persona = new Persona(key, name, description, context);
        personas.put(key, persona);
        
        // 更新配置文件
        ConfigurationSection personaSection = plugin.getConfig()
            .getConfigurationSection("personas." + key);
        if (personaSection == null) {
            personaSection = plugin.getConfig().createSection("personas." + key);
        }
        personaSection.set("name", name);
        personaSection.set("description", description);
        personaSection.set("context", context);
        plugin.saveConfig();
    }

    /**
     * 删除人设
     */
    public void deletePersona(String key) {
        if (!personas.containsKey(key)) {
            throw new IllegalArgumentException("人设不存在：" + key);
        }

        personas.remove(key);
        
        // 从配置文件中删除
        plugin.getConfig().set("personas." + key, null);
        plugin.saveConfig();
    }
} 