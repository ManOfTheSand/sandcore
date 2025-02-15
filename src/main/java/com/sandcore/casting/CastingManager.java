package com.sandcore.casting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.sandcore.classes.ClassDefinition;
import com.sandcore.classes.ClassManager;
import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.levels.LevelManager;

public class CastingManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final ClassManager classManager;
    private final PlayerDataManager playerDataManager;
    private final LevelManager levelManager;
    
    // Track casting state and accumulated combo for each player.
    private final Map<UUID, Boolean> castingMode = new HashMap<>();
    private final Map<UUID, StringBuilder> clickCombos = new HashMap<>();
    private final Map<UUID, BukkitTask> comboTasks = new HashMap<>();
    // Timeout (in ticks) before processing a click combo.
    private final long comboTimeout = 10L; // 0.5 seconds
    
    // Global casting feedback messages (loaded from config.yml).
    private final String enterMessage;
    private final String exitMessage;
    private final String invalidComboMessage;
    private final String insufficientLevelMessage;
    private final String castMessage;
    // Sound settings (also from config.yml).
    private final Sound enterSound;
    private final Sound exitSound;
    private final Sound castSound;
    
    public CastingManager(JavaPlugin plugin, ClassManager classManager, PlayerDataManager playerDataManager, LevelManager levelManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.classManager = classManager;
        this.playerDataManager = playerDataManager;
        this.levelManager = levelManager;
        
        // Load casting feedback and sound settings from config.yml under "casting"
        FileConfiguration config = plugin.getConfig();
        this.enterMessage = config.getString("casting.feedback.enterMessage", "You have entered casting mode!");
        this.exitMessage = config.getString("casting.feedback.exitMessage", "You have exited casting mode.");
        this.invalidComboMessage = config.getString("casting.feedback.invalidCombo", "Invalid casting combo: {combo}");
        this.insufficientLevelMessage = config.getString("casting.feedback.insufficientLevel", "You must be at least level {minLevel} to cast {skill}.");
        this.castMessage = config.getString("casting.feedback.castMessage", "Casting spell: {skill}!");
        
        this.enterSound = getSoundOrDefault(config.getString("casting.sounds.enter", "BLOCK_NOTE_BLOCK_PLING"), Sound.BLOCK_NOTE_BLOCK_PLING);
        this.exitSound = getSoundOrDefault(config.getString("casting.sounds.exit", "BLOCK_NOTE_BLOCK_BASS"), Sound.BLOCK_NOTE_BLOCK_BASS);
        this.castSound = getSoundOrDefault(config.getString("casting.sounds.cast", "ENTITY_ENDER_DRAGON_GROWL"), Sound.ENTITY_ENDER_DRAGON_GROWL);
    }
    
    private Sound getSoundOrDefault(String soundName, Sound def) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (Exception e) {
            return def;
        }
    }
    
    /**
     * Inner class representing a casting ability.
     * (Ensure that your classes.yml defines an "abilities" section for each class and that ClassDefinition.getAbilities()
     * returns a Map<String, CastingAbility> in which the key is the click combo—for example, "L" or "L,R".)
     */
    public static class CastingAbility {
        private final String skill;
        private final int minLevel;
        
        public CastingAbility(String skill, int minLevel) {
            this.skill = skill;
            this.minLevel = minLevel;
        }
        
        public String getSkill() {
            return skill;
        }
        
        public int getMinLevel() {
            return minLevel;
        }
    }
    
    /**
     * Toggles casting mode for the given player.
     */
    public void toggleCastingMode(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = castingMode.getOrDefault(uuid, false);
        boolean newState = !current;
        castingMode.put(uuid, newState);
        if (newState) {
            player.sendMessage(translate(enterMessage));
            player.playSound(player.getLocation(), enterSound, 1.0F, 1.0F);
        } else {
            player.sendMessage(translate(exitMessage));
            player.playSound(player.getLocation(), exitSound, 1.0F, 1.0F);
            clickCombos.remove(uuid);
            if (comboTasks.containsKey(uuid)) {
                comboTasks.get(uuid).cancel();
                comboTasks.remove(uuid);
            }
        }
        logger.info("Casting mode for player " + player.getName() + " set to " + newState);
    }
    
    /**
     * Returns true if the player is currently in casting mode.
     */
    public boolean isInCastingMode(Player player) {
        return castingMode.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Registers a click action (either "L" for left or "R" for right) for a player in casting mode.
     * Accumulates the clicks into a combo string and schedules its processing.
     */
    public void registerClick(Player player, String clickType) {
        UUID uuid = player.getUniqueId();
        if (!isInCastingMode(player))
            return;
        StringBuilder combo = clickCombos.getOrDefault(uuid, new StringBuilder());
        if (combo.length() > 0) {
            combo.append(",");
        }
        combo.append(clickType);
        clickCombos.put(uuid, combo);
        
        // Check if the combo now has exactly three clicks.
        String comboStr = combo.toString();
        int count = comboStr.split(",").length;
        if (count == 3) {
            // Cancel any existing scheduled task for this player.
            if (comboTasks.containsKey(uuid)) {
                comboTasks.get(uuid).cancel();
                comboTasks.remove(uuid);
            }
            processCombo(player, comboStr);
            clickCombos.remove(uuid);
            return;
        }
        
        // Cancel any existing scheduled task for this player.
        if (comboTasks.containsKey(uuid)) {
            comboTasks.get(uuid).cancel();
        }
        
        // Schedule processing after the timeout.
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            processCombo(player, combo.toString());
            clickCombos.remove(uuid);
            comboTasks.remove(uuid);
        }, comboTimeout);
        comboTasks.put(uuid, task);
    }
    
    /**
     * Processes the completed click combo for the player.
     * Checks the player's selected class (from PlayerData) and then looks up the combo in the class's abilities.
     */
    private void processCombo(Player player, String combo) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerDataManager.getPlayerData(uuid);
        String selectedClass = data.getSelectedClass();
        if (selectedClass.isEmpty()) {
            player.sendMessage("You have not selected a class. Unable to cast spells!");
            return;
        }
        // Get the player's class definition (ensure class IDs are handled case-insensitively).
        ClassDefinition def = classManager.getClassDefinition(selectedClass.toLowerCase());
        if (def == null) {
            player.sendMessage("Your class definition could not be found. Please check classes.yml.");
            return;
        }
        // Retrieve the mapping of casting abilities for this class.
        // (Your ClassDefinition should now include an "abilities" section and a corresponding getter.)
        Map<String, CastingAbility> abilities = def.getAbilities();
        if (abilities == null || abilities.isEmpty()) {
            player.sendMessage("No casting abilities defined for your class.");
            return;
        }
        CastingAbility ability = abilities.get(combo.toUpperCase());
        if (ability == null) {
            player.sendMessage(translate(invalidComboMessage.replace("{combo}", combo)));
            return;
        }
        if (data.getLevel() < ability.getMinLevel()) {
            player.sendMessage(translate(insufficientLevelMessage
                    .replace("{minLevel}", String.valueOf(ability.getMinLevel()))
                    .replace("{skill}", ability.getSkill())));
            return;
        }
        executeAbility(player, ability.getSkill());
    }
    
    /**
     * Executes the casting ability (spell) for the player.
     * (Replace the call below with the actual MythicMobs API integration if desired.)
     */
    private void executeAbility(Player player, String skillName) {
        player.sendMessage(translate(castMessage.replace("{skill}", skillName)));
        player.playSound(player.getLocation(), castSound, 1.0F, 1.0F);
        logger.info("Player " + player.getName() + " cast spell: " + skillName);
        // TODO: Replace the following placeholder with an actual API call for casting the spell.
        // MythicMobs.inst().getAPI().castSpell(player, skillName);
    }
    
    private String translate(String message) {
        // Use your ChatUtil to convert custom color codes (hex, gradient, etc.) to Minecraft format.
        return com.sandcore.util.ChatUtil.translateColors(message);
    }
} 