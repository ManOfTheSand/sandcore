package com.sandcore.classes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.casting.CastingManager;
import com.sandcore.util.ChatUtil;

/**
 * ClassManager is responsible for loading and managing player class definitions
 * from classes.yml and for storing/retrieving players' chosen classes.
 */
public class ClassManager {

    // NEW: Define the PlayerClass type that holds per-player class info.
    public static class PlayerClass {
        private String id;
        private Map<String, String> keyCombos;
        private String castingSoundName;
        private float castingSoundVolume;
        private float castingSoundPitch;
        
        public PlayerClass(String id, Map<String, String> keyCombos, String castingSoundName, float castingSoundVolume, float castingSoundPitch) {
            this.id = id;
            this.keyCombos = keyCombos;
            this.castingSoundName = castingSoundName;
            this.castingSoundVolume = castingSoundVolume;
            this.castingSoundPitch = castingSoundPitch;
        }
        
        public String getId() {
            return id;
        }
        
        public Map<String, String> getKeyCombos() {
            return keyCombos;
        }
        
        public String getCastingSoundName() {
            return castingSoundName;
        }
        
        public float getCastingSoundVolume() {
            return castingSoundVolume;
        }
        
        public float getCastingSoundPitch() {
            return castingSoundPitch;
        }
    }

    private JavaPlugin plugin;
    // Map of class definitions from the configuration (key: class id).
    private Map<String, ClassDefinition> classes = new HashMap<>();

    // Map for player's selected classes (key: player's UUID).
    private Map<UUID, String> playerClasses = new HashMap<>();
    private File playerClassFile;
    private YamlConfiguration playerClassConfig;

    public ClassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadClasses();
        loadPlayerClasses();
        plugin.getLogger().info("ClassManager initialized.");
    }

    /**
     * Loads class definitions from classes.yml.
     * The YAML file must have a section "classes" with keys for each class.
     */
    public void loadClasses() {
        try {
            // Ensure classes.yml exists in the plugin data folder.
            File classesFile = new File(plugin.getDataFolder(), "classes.yml");
            if (!classesFile.exists()) {
                plugin.saveResource("classes.yml", false);
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(classesFile);
            if (config.contains("classes")) {
                for (String key : config.getConfigurationSection("classes").getKeys(false)) {
                    ConfigurationSection sec = config.getConfigurationSection("classes." + key);
                    // Read standard fields (e.g., displayName, material, slot, etc.)
                    String displayName = sec.getString("displayName");
                    String lore = sec.getString("lore");
                    String material = sec.getString("material");
                    int slot = sec.getInt("slot");
                    ClassDefinition classDefinition = new ClassDefinition(key, displayName, lore, material, slot);
                    
                    // Parse abilities section for this class (if it exists).
                    if (sec.isConfigurationSection("abilities")) {
                        ConfigurationSection abilitiesSection = sec.getConfigurationSection("abilities");
                        Map<String, CastingManager.CastingAbility> abilities = new HashMap<>();
                        for (String comboKey : abilitiesSection.getKeys(false)) {
                            String skill = abilitiesSection.getString(comboKey + ".skill", "");
                            int minLevel = abilitiesSection.getInt(comboKey + ".minLevel", 0);
                            abilities.put(comboKey.toUpperCase(), new CastingManager.CastingAbility(skill, minLevel));
                            
                            // Debug log: show that the ability is registered.
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("Registered ability for class " + key.toLowerCase() +
                                    " :: Combo: " + comboKey + " -> Skill: " + skill + ", minLevel: " + minLevel);
                            }
                        }
                        classDefinition.setAbilities(abilities);
                    }
                    
                    // Store or register the class definition as per your implementation.
                    classes.put(key.toLowerCase(), classDefinition);
                    plugin.getLogger().info("Loaded class definition: " + key);
                }
            } else {
                plugin.getLogger().severe("No classes defined in classes.yml!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading classes from classes.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the ClassDefinition associated with the given classId.
     *
     * @param classId The unique identifier of a class.
     * @return ClassDefinition, or null if not found.
     */
    public ClassDefinition getClassDefinition(String classId) {
        return classes.get(classId);
    }

    /**
     * Provides a map of all loaded class definitions.
     *
     * @return Map of classes.
     */
    public Map<String, ClassDefinition> getAllClasses() {
        return classes;
    }

    /**
     * Returns the player's selected class.
     *
     * @param player The player.
     * @return The ClassDefinition of the player's class, or null if not set.
     */
    public PlayerClass getPlayerClass(Player player) {
        String classId = playerClasses.get(player.getUniqueId());
        if (classId != null) {
            ClassDefinition def = classes.get(classId);
            if (def == null) {
                return null;
            }
            // In a real implementation, you would load actual values (including sounds) from your configuration.
            return new PlayerClass(classId, def.getKeyCombos(), 
                    "BLOCK_NOTE_BLOCK_PLING", 1.0f, 1.0f);
        }
        return null;
    }

    /**
     * Sets the player's class and saves the choice persistently.
     *
     * @param player  The player.
     * @param classId The unique identifier for the selected class.
     */
    public void setPlayerClass(Player player, String classId) {
        if (!classes.containsKey(classId)) {
            plugin.getLogger().warning("Attempted to set invalid class: " + classId + " for player " + player.getName());
            return;
        }
        // Update in-memory mapping.
        playerClasses.put(player.getUniqueId(), classId);
        // Update persistent storage.
        playerClassConfig.set(player.getUniqueId().toString(), classId);
        try {
            playerClassConfig.save(playerClassFile);
            plugin.getLogger().info("Player " + player.getName() + " set to class " + classId);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving player class data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads players' class assignments from playerclasses.yml.
     */
    private void loadPlayerClasses() {
        try {
            playerClassFile = new File(plugin.getDataFolder(), "playerclasses.yml");
            if (!playerClassFile.exists()) {
                // Create the file if it does not exist.
                playerClassFile.getParentFile().mkdirs();
                playerClassFile.createNewFile();
            }
            playerClassConfig = YamlConfiguration.loadConfiguration(playerClassFile);
            for (String key : playerClassConfig.getKeys(false)) {
                playerClasses.put(UUID.fromString(key), playerClassConfig.getString(key));
            }
            plugin.getLogger().info("Loaded player class assignments.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player class assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the formatted display name for the given class key.
     * If the class doesn't exist, the raw key is returned.
     */
    public String getFormattedClassName(String classKey) {
        if (classes != null && classes.containsKey(classKey)) {
            // Assume ClassDefinition has a method getDisplayName() returning the raw display name.
            String rawName = classes.get(classKey).getDisplayName();
            return ChatUtil.translateColors(rawName);
        }
        return classKey;
    }
} 