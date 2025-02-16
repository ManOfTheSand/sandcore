package com.sandcore.casting;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.SandCore;

/**
 * CastingSystem emulates a Wynncraft-like skills casting system.
 * It listens for players pressing the F key to activate casting mode,
 * asynchronously detects a three-click combo (L/R clicks) within a timeout,
 * determines the MythicMob skill based on the player's selected class (as defined in classes.yml),
 * and casts the skill using the MythicMob API.
 *
 * Configuration (loaded from classes.yml):
 * - casting.timeout: combo timeout in seconds (default: 5)
 * - casting.activationMessage: Action bar message (with hex colors) when casting mode is activated.
 * - casting.cancelMessage: Action bar message when casting is cancelled (e.g. timeout or invalid combo).
 * - casting.successMessage: Action bar message on a successful cast.
 * - casting.activationSound, casting.cancelSound, casting.successSound: Sound names.
 * - casting.comboMappings: For each class (e.g. "Mage", "Warrior", "Rogue"), defines a mapping
 *   of a three-click combo pattern (string such as "L,R,L") to a MythicMob skill name.
 */
public class CastingSystem implements Listener {

    private final SandCore plugin;
    // Configurable options for casting
    private int comboTimeoutSeconds;
    private String activationMessage;
    private String cancelMessage;
    private String successMessage;
    private String activationSound;
    private String cancelSound;
    private String successSound;
    // Mapping: Player's class => combo pattern (e.g., "L,R,L") => MythicMob skill name.
    private Map<String, Map<String, String>> comboMappings;
    // Active casting sessions keyed by player UUID.
    private final Map<UUID, CastingSession> activeSessions = new HashMap<>();

    /**
     * Constructor. Loads the casting configuration from classes.yml
     * and registers this system as an event listener.
     */
    public CastingSystem(SandCore plugin) {
        this.plugin = plugin;
        loadConfiguration();
        // Register event listeners for casting events.
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Loads casting configuration from the "casting" section in classes.yml.
     */
    private void loadConfiguration() {
        try {
            File classesFile = new File(plugin.getDataFolder(), "classes.yml");
            if (!classesFile.exists()) {
                plugin.saveResource("classes.yml", false);
            }
            YamlConfiguration classesConfig = YamlConfiguration.loadConfiguration(classesFile);
            if (!classesConfig.contains("casting")) {
                plugin.getLogger().warning("Casting section missing in classes.yml!");
                return;
            }
            // Use the casting section for all configurable options.
            YamlConfiguration castingConf = (YamlConfiguration) classesConfig.getConfigurationSection("casting");
            this.comboTimeoutSeconds = castingConf.getInt("timeout", 5); // Default 5 seconds timeout.
            this.activationMessage = castingConf.getString("activationMessage", "&x&F&F&C&C&C&C Casting Mode Activated!");
            this.cancelMessage = castingConf.getString("cancelMessage", "&x&F&F&3&3&3&3 Casting Cancelled!");
            this.successMessage = castingConf.getString("successMessage", "&x&A&A&D&D&F&F Skill Cast Successful!");
            this.activationSound = castingConf.getString("activationSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            this.cancelSound = castingConf.getString("cancelSound", "ENTITY_BLAZE_HURT");
            this.successSound = castingConf.getString("successSound", "ENTITY_PLAYER_LEVELUP");
            comboMappings = new HashMap<>();
            if (castingConf.contains("comboMappings")) {
                // For each class (Mage, Warrior, Rogue, etc.) load its combo mappings.
                for (String className : castingConf.getConfigurationSection("comboMappings").getKeys(false)) {
                    Map<String, String> mapping = new HashMap<>();
                    for (String combo : castingConf.getConfigurationSection("comboMappings." + className).getKeys(false)) {
                        String skillName = castingConf.getString("comboMappings." + className + "." + combo);
                        mapping.put(combo, skillName);
                    }
                    comboMappings.put(className, mapping);
                }
            }
            plugin.getLogger().info("Casting configuration loaded successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading casting configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Event handler that detects when a player presses the F key (swap hand items event)
     * to activate casting mode.
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // If the player is already in a casting session, do nothing.
        if (activeSessions.containsKey(player.getUniqueId())) {
            return;
        }
        // Cancel the default item swap action.
        event.setCancelled(true);
        // Activate casting mode.
        activateCastingMode(player);
    }

    /**
     * Event handler to detect left/right clicks while in casting mode.
     * These clicks are recorded as part of the three-click combo.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) {
            return;
        }
        // Only process left/right click actions.
        Action action = event.getAction();
        String clickType = null;
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            clickType = "L";
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            clickType = "R";
        }
        if (clickType == null) {
            return;
        }
        // Record the click in the player's casting session.
        CastingSession session = activeSessions.get(player.getUniqueId());
        session.addClick(clickType);
        plugin.getLogger().info("Player " + player.getName() + " clicked: " + clickType + " (Combo: " + session.getComboString() + ")");
        // When exactly three clicks have been recorded, process the combo.
        if (session.getComboSize() == 3) {
            session.cancelTimeout();
            processCombo(player, session.getComboString());
            activeSessions.remove(player.getUniqueId());
        }
    }

    /**
     * Activates casting mode for the specified player.
     * Displays an action bar message and plays the activation sound, then starts the timeout.
     */
    private void activateCastingMode(Player player) {
        CastingSession session = new CastingSession(player);
        activeSessions.put(player.getUniqueId(), session);
        // Show the activation action bar message and play sound on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendActionBar(translateHexColors(activationMessage));
            playSound(player, activationSound);
        });
        plugin.getLogger().info("Casting mode activated for player: " + player.getName());
        // Schedule a timeout task that cancels the combo if not completed in time.
        session.startTimeoutTask(comboTimeoutSeconds);
    }

    /**
     * Processes the three-click combo: determines the player's class, looks up the corresponding
     * MythicMob skill mapping, and attempts to cast the skill.
     */
    private void processCombo(Player player, String combo) {
        // Retrieve the player's selected class via the ClassManager.
        String selectedClass = plugin.getClassManager().getSelectedClass(player);
        if (selectedClass == null) {
            plugin.getLogger().warning("Player " + player.getName() + " does not have a selected class.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar("§cNo class selected!");
                playSound(player, cancelSound);
            });
            return;
        }
        // Find the combo mapping for this class.
        Map<String, String> mappings = comboMappings.get(selectedClass);
        if (mappings == null || !mappings.containsKey(combo)) {
            plugin.getLogger().info("No valid skill mapping for combo " + combo + " for class " + selectedClass);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound);
            });
            return;
        }
        String skillName = mappings.get(combo);
        // Attempt to cast the MythicMob skill (integration with MythicMob API goes here).
        boolean castSuccess = castMythicMobSkill(player, skillName);
        if (castSuccess) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar(translateHexColors(successMessage));
                playSound(player, successSound);
            });
            plugin.getLogger().info("Player " + player.getName() + " successfully cast " + skillName + " using combo " + combo);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound);
            });
            plugin.getLogger().warning("Failed to cast skill " + skillName + " for player " + player.getName());
        }
    }

    /**
     * Placeholder method for MythicMob skill casting integration.
     * Replace this with the actual integration call from the MythicMob API.
     *
     * @param player    The player casting the skill.
     * @param skillName Name of the MythicMob skill to cast.
     * @return true if the cast was successful, false otherwise.
     */
    private boolean castMythicMobSkill(Player player, String skillName) {
        try {
            // Example (pseudo-code): MythicMobs.inst().getSkillManager().castSkill(player, skillName, player.getLocation());
            plugin.getLogger().info("Casting MythicMob skill '" + skillName + "' for player " + player.getName());
            // Simulate a successful cast.
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error casting skill: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to play a sound at the player's location.
     * The sound is obtained via its name from configuration.
     */
    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            // Use default volume and pitch; these could also be made configurable.
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound name: " + soundName);
        }
    }

    /**
     * Helper method to translate hex color codes (using & as the prefix) to Bukkit ChatColor.
     */
    private String translateHexColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Inner class representing an active casting session.
     * It tracks the player's click sequence and schedules a timeout to cancel incomplete combos.
     */
    private class CastingSession {
        private final Player player;
        private final List<String> clicks = new ArrayList<>();
        private int taskId = -1;

        public CastingSession(Player player) {
            this.player = player;
        }

        /**
         * Adds a click (either "L" or "R") to the current combo.
         */
        public void addClick(String click) {
            if (clicks.size() < 3) {
                clicks.add(click);
            }
        }

        /**
         * Returns the number of clicks recorded.
         */
        public int getComboSize() {
            return clicks.size();
        }

        /**
         * Returns the combo as a comma-separated string (e.g., "L,R,L").
         */
        public String getComboString() {
            return String.join(",", clicks);
        }

        /**
         * Starts a timeout task that will cancel the casting session if the player
         * does not complete the three-click combo within the specified timeout.
         */
        public void startTimeoutTask(int timeoutSeconds) {
            taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                activeSessions.remove(player.getUniqueId());
                plugin.getLogger().info("Casting combo timeout for player: " + player.getName());
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound);
            }, timeoutSeconds * 20L).getTaskId();
        }

        /**
         * Cancels the previously scheduled timeout for this casting session.
         */
        public void cancelTimeout() {
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
    }
} 