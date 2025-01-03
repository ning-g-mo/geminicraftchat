package cn.ningmo.geminicraftchat.persona;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PersonaManager {
    private final GeminiCraftChat plugin;
    private final Map<UUID, String> playerPersonas;
    
    public PersonaManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.playerPersonas = new HashMap<>();
    }
    
    public void setPersona(Player player, String personaName) {
        if (!personaExists(personaName)) {
            throw new IllegalArgumentException("人设不存在！");
        }
        playerPersonas.put(player.getUniqueId(), personaName);
    }
    
    public String getPersona(Player player) {
        return playerPersonas.getOrDefault(player.getUniqueId(), "default");
    }
    
    public boolean personaExists(String name) {
        ConfigurationSection personas = plugin.getConfig().getConfigurationSection("personas");
        return personas != null && personas.contains(name);
    }
    
    public void createPersona(String name, String description) {
        ConfigurationSection personas = plugin.getConfig().getConfigurationSection("personas");
        if (personas == null) {
            personas = plugin.getConfig().createSection("personas");
        }
        
        ConfigurationSection persona = personas.createSection(name);
        persona.set("name", name);
        persona.set("description", description);
        plugin.saveConfig();
    }
} 