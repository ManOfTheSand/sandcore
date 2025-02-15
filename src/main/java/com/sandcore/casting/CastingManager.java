package com.sandcore.casting;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.sandcore.classes.ClassDefinition;
import com.sandcore.classes.ClassManager;

import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.bukkit.MythicBukkit;

public class CastingManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<UUID, CastingState> castingPlayers = new HashMap<>();
    private int timeoutSeconds = 5;
    // Casting sound when entering casting mode.
    private String enterSoundName = "BLOCK_NOTE_BLOCK_PLING";
    private float enterSoundVolume = 1.0F;
    private float enterSoundPitch = 1.0F;
    private final ClassManager classManager;
    
    // ---------------------------------------------------------------------------------
    // Newly added inner class to satisfy references from ClassManager and others.
    // You can later extend this stub with additional properties or behaviors as needed.
    public static class CastingAbility {
        // Stub implementation for casting ability functionality.
    }
    // ---------------------------------------------------------------------------------

    public CastingManager(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.classManager = classManager;
        loadConfiguration();
    }

    private class CastingState {
        StringBuilder combo = new StringBuilder();
        BukkitTask timeoutTask;
        com.sandcore.classes.ClassManager.PlayerClass playerClass;
    }

    public void loadConfiguration() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                
                // Load casting timeout
                timeoutSeconds = config.getInt("casting.timeout", 5);
                
                // Load enter sound configuration
                if (config.isConfigurationSection("casting.enter_sound")) {
                    enterSoundName = config.getString("casting.enter_sound.name", "BLOCK_NOTE_BLOCK_PLING");
                    enterSoundVolume = (float) config.getDouble("casting.enter_sound.volume", 1.0);
                    enterSoundPitch = (float) config.getDouble("casting.enter_sound.pitch", 1.0);
                }
                
                // (Key combos are now defined per class in classes.yml – see ClassManager)
                logger.info("Casting timeout: " + timeoutSeconds + " seconds, "
                    + "enter sound: " + enterSoundName + " (" + enterSoundVolume + "/" + enterSoundPitch + ")");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load casting configuration", e);
            }
        });
    }

    public void toggleCastingMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (castingPlayers.containsKey(uuid)) {
            cancelCasting(player);
            player.sendMessage("§cCasting mode disabled");
        } else {
            ClassManager.PlayerClass playerClass = this.classManager.getPlayerClass(player);
            if (playerClass == null) {
                player.sendMessage(ChatColor.RED + "No class assigned.");
                return;
            }
            Map<?, ?> keyCombos = playerClass.getKeyCombos();
            if (keyCombos == null || keyCombos.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Your class does not support any key combos.");
                return;
            }

            CastingState state = new CastingState();
            state.playerClass = playerClass;
            castingPlayers.put(uuid, state);
            player.sendMessage("§aCasting mode enabled - Perform a 3-click combo!");
            logger.fine("toggleCastingMode: Enabled casting mode for player " + player.getName() + " (class: " + state.playerClass.getId() + ")");

            // Play class-specific casting sound from player's class config.
            try {
                String classSound = playerClass.getCastingSoundName();
                float classSoundVol = playerClass.getCastingSoundVolume();
                float classSoundPitch = playerClass.getCastingSoundPitch();
                player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(classSound), classSoundVol, classSoundPitch);
                logger.fine("toggleCastingMode: Played class-specific casting sound (" + classSound + ") for player " + player.getName());
            } catch (Exception ex) {
                player.sendMessage("§cError playing casting sound.");
                logger.warning("toggleCastingMode: Error playing casting sound for player " + player.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void cancelCasting(Player player) {
        CastingState state = castingPlayers.remove(player.getUniqueId());
        if (state != null && state.timeoutTask != null) {
            state.timeoutTask.cancel();
        }
        // Update GUI through your existing GUI system
    }

    public void handleClick(Player player, ClickType clickType) {
        CastingState state = castingPlayers.get(player.getUniqueId());
        if (state == null) return;

        // Start timeout on first click
        if (state.combo.length() == 0) {
            state.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cancelCasting(player);
                player.sendMessage("§cCast timed out!");
            }, timeoutSeconds * 20L);
        }

        // Record click and update action bar with current combo keys
        state.combo.append(clickType == ClickType.LEFT ? "L" : "R");
        player.sendActionBar(net.kyori.adventure.text.Component.text("Combo: " + state.combo.toString()));
        
        if (state.combo.length() == 3) {
            completeCast(player, state);
        }
    }

    private void completeCast(Player player, CastingState state) {
        try {
            String combo = state.combo.toString();
            logger.fine("completeCast: Player " + player.getName() + " performed combo: " + combo);

            // Retrieve key combos from the player's class (loaded from classes.yml)
            java.util.Map<String, String> keyCombos = state.playerClass.getKeyCombos();
            String skillName = keyCombos.get(combo);

            if (skillName != null) {
                Skill skill = MythicBukkit.inst().getSkillManager().getSkill(skillName).orElse(null);
                if (skill != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MythicBukkit.inst().getAPIHelper().castSkill(player, skillName);
                        player.sendMessage("§aCasted: §e" + skillName);
                        logger.fine("completeCast: Successfully cast skill: " + skillName + " for player " + player.getName());
                    });
                } else {
                    player.sendMessage("§cInvalid skill configuration");
                    logger.warning("completeCast: Skill " + skillName + " not found for player " + player.getName());
                }
            } else {
                player.sendMessage("§cInvalid combo for your class");
                logger.warning("completeCast: Combo " + combo + " not valid for class " + state.playerClass.getId() + " (player " + player.getName() + ")");
            }
        } finally {
            // Instead of canceling casting mode, reset the combo and reschedule timeout.
            if (state.timeoutTask != null) {
                state.timeoutTask.cancel();
            }
            state.combo.setLength(0); // reset combo
            state.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cancelCasting(player);
                player.sendMessage("§cCasting mode timed out!");
            }, timeoutSeconds * 20L);
            player.sendActionBar(net.kyori.adventure.text.Component.text("Combo: "));
            logger.fine("completeCast: Combo reset for player " + player.getName());
        }
    }

    public enum ClickType {
        LEFT, RIGHT
    }

    /**
     * Returns true if the player is currently in casting mode.
     */
    public boolean isCasting(Player player) {
         return castingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Checks if the skill with the given id is unlocked for the player's class.
     *
     * @param player the player attempting to cast.
     * @param skillId the skill id to check.
     * @return true if the player's class has unlocked this skill, false otherwise.
     */
    public boolean isSkillUnlocked(Player player, String skillId) {
        // Retrieve player's chosen class via the ClassManager.
        ClassManager.PlayerClass pClass = classManager.getPlayerClass(player);
        if (pClass == null) return false;
        ClassDefinition def = classManager.getClassDefinition(pClass.getId());
        if (def == null) return false;
        // Check if any key combo value matches the provided skillId.
        Map<?, ?> keyCombos = def.getKeyCombos();
        if (keyCombos == null) {
            return false;
        }
        return keyCombos.values().stream().anyMatch(id -> id.equalsIgnoreCase(skillId));
    }

    public ClassManager getClassManager() {
        return this.classManager;
    }

    /**
     * Reloads the casting configuration.
     */
    public void reloadCastingConfig() {
        loadConfiguration();
    }
} 