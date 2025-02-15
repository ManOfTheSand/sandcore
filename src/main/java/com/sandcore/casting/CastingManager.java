package com.sandcore.casting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.sandcore.classes.ClassManager;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.levels.LevelManager;

/**
 * CastingManager is responsible for handling the simplified casting system.
 * 
 * A player presses F (detected via the PlayerSwapHandItemsEvent) to enter casting mode.
 * Once in casting mode, the next three left/right clicks (collected via PlayerInteractEvent)
 * are recorded. Each click is displayed on the action bar and a sound is played.
 * If three clicks are not completed within the allowed timeframe the combo is cancelled.
 */
public class CastingManager implements Listener {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final ClassManager classManager;
    private final PlayerDataManager playerDataManager;
    private final LevelManager levelManager;
    
    // Store casting data for each player currently in casting mode.
    private Map<UUID, CastingData> castingPlayers = new HashMap<>();
    // Timeout delay in ticks for each combo click (e.g., 40 ticks = 2 seconds).
    private final long TIMEOUT_DELAY = 40L;
    
    // Global casting feedback messages (loaded from config.yml).
    private String enterMessage;
    private String exitMessage;
    private String invalidComboMessage;
    private String insufficientLevelMessage;
    private String castMessage;
    // Sound settings (also from config.yml).
    private Sound enterSound;
    private Sound exitSound;
    private Sound castSound;
    
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
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.info("CastingManager initialized with simplified casting system.");
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
    
    // Inner class that holds a player's current combo and timeout task.
    private class CastingData {
        List<String> combo = new ArrayList<>();
        BukkitTask timeoutTask;
    }
    
    /**
     * Starts casting mode for the player.
     *
     * @param player The player entering casting mode.
     */
    private void startCasting(Player player) {
        if (castingPlayers.containsKey(player.getUniqueId())) {
            // Player is already in casting mode.
            return;
        }
        CastingData data = new CastingData();
        castingPlayers.put(player.getUniqueId(), data);
        // Start a timeout in case the player does not complete the combo.
        data.timeoutTask = scheduleTimeout(player);
        sendActionBar(player, "Casting mode enabled. Awaiting combo: ");
        logger.info(player.getName() + " has entered casting mode.");
    }
    
    /**
     * Schedules a timeout task to cancel the casting combo if not completed in time.
     *
     * @param player The player in casting mode.
     * @return The BukkitTask of the scheduled timeout.
     */
    private BukkitTask scheduleTimeout(final Player player) {
        return Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (castingPlayers.containsKey(player.getUniqueId())) {
                castingPlayers.remove(player.getUniqueId());
                sendActionBar(player, "Casting cancelled due to timeout.");
                logger.info("Casting combo timed out for " + player.getName());
            }
        }, TIMEOUT_DELAY);
    }
    
    /**
     * Utility method to send an action bar message to the player.
     *
     * @param player  The target player.
     * @param message The message to display.
     */
    private void sendActionBar(Player player, String message) {
        player.sendActionBar(message);
    }
    
    /**
     * Listens for player clicks while in casting mode.
     * Records left/right clicks, updates the action bar, plays a sound,
     * and processes the combo once three clicks are recorded.
     *
     * @param event The player interact event.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!castingPlayers.containsKey(uuid)) {
            return; // Not in casting mode.
        }
        
        // Process only left or right clicks.
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        event.setCancelled(true); // Prevent any default interaction effects during casting.
        CastingData data = castingPlayers.get(uuid);
        
        // Restart the timeout for combo continuation.
        if (data.timeoutTask != null) {
            data.timeoutTask.cancel();
        }
        data.timeoutTask = scheduleTimeout(player);
        
        // Determine click type: "L" for left, "R" for right.
        String clickType = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) ? "L" : "R";
        data.combo.add(clickType);
        
        // Build and display the current combo in the action bar.
        StringBuilder comboDisplay = new StringBuilder("Casting Combo: ");
        for (String s : data.combo) {
            comboDisplay.append("[").append(s).append("] ");
        }
        sendActionBar(player, comboDisplay.toString());
        
        // Play a pling sound for each click.
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        // When the combo reaches exactly 3 clicks, process the combo.
        if (data.combo.size() == 3) {
            data.timeoutTask.cancel();
            processCastingCombo(player, data.combo);
            // Remove the player from casting mode.
            castingPlayers.remove(uuid);
        }
    }
    
    /**
     * Processes the complete casting combo.
     * In this simplified system the combo is simply logged and a message is sent,
     * but you can add additional logic here to trigger spells or abilities.
     *
     * @param player The player completing the combo.
     * @param combo  The list representing the combo (e.g., ["L", "R", "L"]).
     */
    private void processCastingCombo(Player player, List<String> combo) {
        StringBuilder executedCombo = new StringBuilder();
        for (String s : combo) {
            executedCombo.append(s);
        }
        // Notify the player and log the completed combo.
        player.sendMessage("Casting complete! Your combo: " + executedCombo.toString());
        sendActionBar(player, "Casting complete!");
        logger.info(player.getName() + " executed casting combo: " + executedCombo.toString());
        
        // TODO: Integrate proper ability/spell activation here.
    }
    
    /**
     * Reloads the casting configuration from the plugin's config.yml.
     */
    public void reloadCastingConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enterMessage = config.getString("casting.feedback.enterMessage", "You have entered casting mode!");
        this.exitMessage = config.getString("casting.feedback.exitMessage", "You have exited casting mode.");
        this.invalidComboMessage = config.getString("casting.feedback.invalidCombo", "Invalid casting combo: {combo}");
        this.insufficientLevelMessage = config.getString("casting.feedback.insufficientLevel", "You must be at least level {minLevel} to cast {skill}.");
        this.castMessage = config.getString("casting.feedback.castMessage", "Casting spell: {skill}!");
        
        this.enterSound = getSoundOrDefault(config.getString("casting.sounds.enter", "BLOCK_NOTE_BLOCK_PLING"), Sound.BLOCK_NOTE_BLOCK_PLING);
        this.exitSound = getSoundOrDefault(config.getString("casting.sounds.exit", "BLOCK_NOTE_BLOCK_BASS"), Sound.BLOCK_NOTE_BLOCK_BASS);
        this.castSound = getSoundOrDefault(config.getString("casting.sounds.cast", "ENTITY_ENDER_DRAGON_GROWL"), Sound.ENTITY_ENDER_DRAGON_GROWL);
        logger.info("Casting configuration reloaded.");
    }
    
    /**
     * Returns true if the player is currently in casting mode.
     *
     * @param player The player to check.
     * @return true if in casting mode, false otherwise.
     */
    public boolean isInCastingMode(Player player) {
        return castingPlayers.containsKey(player.getUniqueId());
    }
    
    /**
     * Manually registers a click for the player's casting combo.
     *
     * @param player    The player casting.
     * @param clickType "L" for left click or "R" for right click.
     */
    public void registerClick(Player player, String clickType) {
        if (!castingPlayers.containsKey(player.getUniqueId())) {
            return;
        }
        CastingData data = castingPlayers.get(player.getUniqueId());
        if (data.timeoutTask != null) {
            data.timeoutTask.cancel();
        }
        data.timeoutTask = scheduleTimeout(player);
        data.combo.add(clickType);
        StringBuilder comboDisplay = new StringBuilder("Casting Combo: ");
        for (String s : data.combo) {
            comboDisplay.append("[").append(s).append("] ");
        }
        sendActionBar(player, comboDisplay.toString());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        if (data.combo.size() == 3) {
            data.timeoutTask.cancel();
            processCastingCombo(player, data.combo);
            castingPlayers.remove(player.getUniqueId());
        }
    }
    
    /**
     * Toggles the casting mode for the given player.
     * If the player is in casting mode, it cancels it; otherwise, it starts casting mode.
     *
     * @param player The player to toggle casting mode.
     */
    public void toggleCastingMode(Player player) {
        if (isInCastingMode(player)) {
            cancelCastingMode(player);
        } else {
            startCasting(player);
            sendActionBar(player, enterMessage);
            player.playSound(player.getLocation(), enterSound, 1.0f, 1.0f);
            logger.info(player.getName() + " has toggled casting mode on.");
        }
    }
    
    /**
     * Cancels casting mode for the given player.
     *
     * @param player The player whose casting mode will be cancelled.
     */
    private void cancelCastingMode(Player player) {
        CastingData data = castingPlayers.remove(player.getUniqueId());
        if (data != null) {
            if (data.timeoutTask != null) {
                data.timeoutTask.cancel();
            }
            sendActionBar(player, exitMessage);
            player.playSound(player.getLocation(), exitSound, 1.0f, 1.0f);
            logger.info(player.getName() + " has toggled casting mode off.");
        }
    }
} 