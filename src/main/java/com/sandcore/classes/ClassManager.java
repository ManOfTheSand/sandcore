package com.sandcore.classes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ClassManager is responsible for loading and managing player class definitions
 * from classes.yml and for storing/retrieving players' chosen classes.
 */
public class ClassManager {

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
                    // Retrieve each attribute for a class.
                    String displayName = config.getString("classes." + key + ".displayName");
                    String lore = config.getString("classes." + key + ".lore");
                    String material = config.getString("classes." + key + ".material");
                    ClassDefinition def = new ClassDefinition(key, displayName, lore, material);
                    classes.put(key, def);
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
    public ClassDefinition getPlayerClass(Player player) {
        String classId = playerClasses.get(player.getUniqueId());
        if (classId != null) {
            return classes.get(classId);
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
} 