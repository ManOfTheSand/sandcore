package com.sandcore.casting;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

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
    private int leftClickLockTicks = 1;  // Default values if not in config
    private int rightClickLockTicks = 4;
    private long comboCooldownMillis = 1000;
    // New configuration for combo click sound
    private String clickSound;
    private double clickSoundVolume;
    private double clickSoundPitch;
    // Mapping: Player's class => combo pattern (e.g., "L,R,L") => MythicMob skill name.
    private Map<String, Map<String, String>> comboMappings;
    // Active casting sessions keyed by player UUID.
    private final Map<UUID, CastingSession> activeSessions = new ConcurrentHashMap<>();
    private final ExecutorService comboExecutor = Executors.newCachedThreadPool();
    private CastingConfig cachedConfig;
    private long lastConfigHash;
    // Cooldown tracking for mode toggling (1 second)
    private final Map<UUID, Instant> toggleCooldowns = new ConcurrentHashMap<>();

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
            long currentHash = getConfigHash(classesFile);
            
            if (cachedConfig == null || currentHash != lastConfigHash) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(classesFile);
                cachedConfig = new CastingConfig(config);
                lastConfigHash = currentHash;
                plugin.getLogger().info("Reloaded and cached casting configuration");
            }
            
            // Update runtime values from cache
            comboTimeoutSeconds = cachedConfig.timeout;
            comboCooldownMillis = cachedConfig.cooldownMillis;
            leftClickLockTicks = cachedConfig.leftClickLock;
            rightClickLockTicks = cachedConfig.rightClickLock;
            activationSound = cachedConfig.activationSound;
            cancelSound = cachedConfig.cancelSound;
            successSound = cachedConfig.successSound;
            clickSound = cachedConfig.clickSound;
            clickSoundVolume = cachedConfig.clickSoundVolume;
            clickSoundPitch = cachedConfig.clickSoundPitch;
            comboMappings = cachedConfig.comboMappings;
            
            // Load messages and other sound config
            activationMessage = cachedConfig.activationMessage;
            cancelMessage = cachedConfig.cancelMessage;
            successMessage = cachedConfig.successMessage;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading casting config: " + e.getMessage());
        }
    }

    /**
     * Event handler that detects when a player presses the F key (swap hand items event)
     * to activate casting mode.
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        // Check cooldown
        if (toggleCooldowns.containsKey(player.getUniqueId())) {
            if (Instant.now().isBefore(toggleCooldowns.get(player.getUniqueId()))) {
                player.sendActionBar(translateHexColors("&cYou must wait before toggling casting mode again!"));
                event.setCancelled(true);
                return;
            }
        }
        
        // Cancel the default item swap action to prevent vanilla behavior.
        event.setCancelled(true);
        // If the player is already in casting mode, cancel it.
        if (activeSessions.containsKey(player.getUniqueId())) {
            CastingSession session = activeSessions.remove(player.getUniqueId());
            session.cancelTimeout();
            // Notify the player that casting mode has been deactivated.
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar("Casting mode deactivated!");
                playSound(player, cancelSound, 1.0f, 1.0f);
                // Set cooldown
                toggleCooldowns.put(player.getUniqueId(), Instant.now().plusSeconds(1));
            });
            return;
        }
        // Otherwise, activate casting mode.
        toggleCooldowns.put(player.getUniqueId(), Instant.now().plusSeconds(1));
        activateCastingMode(player);
    }

    /**
     * Handles left-click attacks on entities (mobs/players) while in casting mode
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        // Record left-click attack as part of combo
        CastingSession session = activeSessions.get(player.getUniqueId());
        session.addClick("L");
        
        // Update action bar and check for combo completion
        player.sendActionBar("Combo: " + session.getComboString());
        plugin.getLogger().info("Player " + player.getName() + " entity left-click (Combo: " + session.getComboString() + ")");
        
        if (session.getComboSize() == 3) {
            session.cancelTimeout();
            processCombo(player, session.getComboString());
        }
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
        // Update the action bar with the current combo.
        player.sendActionBar("Combo: " + session.getComboString());
        plugin.getLogger().info("Player " + player.getName() + " clicked: " + clickType + " (Combo: " + session.getComboString() + ")");
        // When exactly three clicks have been recorded, process the combo.
        if (session.getComboSize() == 3) {
            session.cancelTimeout();
            processCombo(player, session.getComboString());
        }
    }

    /**
     * Activates casting mode for the specified player.
     * Displays an action bar message and plays the activation sound, then starts the timeout.
     */
    private void activateCastingMode(Player player) {
        CastingSession session = new CastingSession(player);
        activeSessions.put(player.getUniqueId(), session);
        
        // Final null check before using
        if (activationSound == null) {
            plugin.getLogger().severe("CRITICAL ERROR: activationSound is null when activating casting mode!");
        }

        // Show the activation action bar message and play sound on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendActionBar(""); // Clear any previous message
            player.sendActionBar(translateHexColors(activationMessage));
            plugin.getLogger().info("activateCastingMode: playing activation sound, sound parameter: '" + activationSound + "'");
            playSound(player, activationSound, 1.0f, 1.0f);
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
        // Retrieve the player's selected class from the player's data.
        String selectedClass = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getSelectedClass();
        if (selectedClass == null) {
            plugin.getLogger().warning("Player " + player.getName() + " does not have a selected class.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar("§cNo class selected!");
                playSound(player, cancelSound, 1.0f, 1.0f);
            });
            return;
        }
        // Try to get the combo mapping from the casting configuration.
        Map<String, String> mappings = comboMappings.get(selectedClass);
        // If no mapping was loaded via the "casting" section,
        // fall back to the keyCombos defined in classes.yml under "classes.<selectedClass>.keyCombos"
        if (mappings == null || mappings.isEmpty()) {
            File classesFile = new File(plugin.getDataFolder(), "classes.yml");
            YamlConfiguration classesConfig = YamlConfiguration.loadConfiguration(classesFile);
            String path = "classes." + selectedClass.toLowerCase() + ".keyCombos";
            if (classesConfig.contains(path)) {
                mappings = new HashMap<>();
                for (String comboKey : classesConfig.getConfigurationSection(path).getKeys(false)) {
                    String skill = classesConfig.getString(path + "." + comboKey);
                    mappings.put(comboKey, skill);
                }
            }
        }
        if (mappings == null || !mappings.containsKey(combo)) {
            plugin.getLogger().info("No valid skill mapping for combo " + combo + " for class " + selectedClass);
            // Reset clicks and start cooldown for invalid combos
            if (activeSessions.containsKey(player.getUniqueId())) {
                activeSessions.get(player.getUniqueId()).resetClicks();
                activeSessions.get(player.getUniqueId()).startCooldown();
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar(translateHexColors(cancelMessage));
                player.sendActionBar(""); // Clear previous combo display
                playSound(player, cancelSound, 1.0f, 1.0f);
            });
            return;
        }
        String skillName = mappings.get(combo);
        // Attempt to cast the MythicMob skill
        boolean castSuccess = castMythicMobSkill(player, skillName);
        if (castSuccess && activeSessions.containsKey(player.getUniqueId())) {
            activeSessions.get(player.getUniqueId()).resetClicks();
            activeSessions.get(player.getUniqueId()).startCooldown();
            plugin.getLogger().info("Player " + player.getName() + " successfully cast " + skillName + " using combo " + combo);
            // Add visual feedback
            long formattedTime = Duration.between(activeSessions.get(player.getUniqueId()).getLastClickTime(), Instant.now()).toMillis();
            player.sendTitle(
                "", 
                translateHexColors("&a&l" + combo + " &r&7(" + formattedTime + "ms)"), 
                5,  // Fade in
                20, // Stay
                5   // Fade out
            );
            // Add particle effects
            player.spawnParticle(
                Particle.HAPPY_VILLAGER, 
                player.getEyeLocation(), 
                5, 0.2, 0.5, 0.2, 0.1
            );
        } else {
            activeSessions.get(player.getUniqueId()).resetClicks();
            activeSessions.get(player.getUniqueId()).startCooldown();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound, 1.0f, 1.0f);
            });
        }
    }

    /**
     * Placeholder method for MythicMob skill casting integration.
     * Replace this with the actual integration call from the MythicMob API.
     *
     * @param player    The player casting the skill.
     * @param skillName Name of the MythicMob skill to cast.
     */
    private boolean castMythicMobSkill(Player player, String skillName) {
        try {
            // Use the player's eye location as the casting location.
            Location castLocation = player.getEyeLocation().clone();
            boolean result = io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().castSkill(player, skillName, castLocation);
            if (result) {
                plugin.getLogger().info("Casting MythicMob skill '" + skillName + "' for player " + player.getName());
            } else {
                plugin.getLogger().warning("MythicMob skill '" + skillName + "' was not successfully cast for player " + player.getName());
            }
            return result;
        } catch (Exception e) {
            plugin.getLogger().severe("Error casting MythicMob skill: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Plays a sound to the player at their location.
     * @param player The player to play the sound to.
     * @param soundName The name of the sound to play
     */
    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isEmpty()) {
            plugin.getLogger().warning("playSound called with null or empty soundName!");
            return;
        }
        plugin.getLogger().info("playSound: playing sound '" + soundName + "'");
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            Location loc = player.getLocation();
            player.playSound(loc, sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName);
        }
    }

    /**
     * Plays the combo click sound for a player's casting click.
     */
    private void playComboClickSound(Player player) {
        if (clickSound == null || clickSound.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(clickSound.toUpperCase());
            player.playSound(player.getLocation(), sound, (float) clickSoundVolume, (float) clickSoundPitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid combo click sound: " + clickSound);
        }
    }

    /**
     * Helper method to translate hex color codes (using & as the prefix) to Bukkit ChatColor.
     * If the provided message is null, returns an empty string.
     */
    private String translateHexColors(String message) {
        if(message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Inner class representing an active casting session.
     * It tracks the player's click sequence and schedules a timeout to cancel incomplete combos.
     */
    private class CastingSession {
        private final Player player;
        private final List<String> clicks = new ArrayList<>();
        private Instant cooldownEnd = Instant.MIN;
        private int taskId = -1;
        // Remove timestamp-based filtering; we now use a click lock.
        private boolean clickLock = false;
        private Instant lastClickTime = Instant.MIN;
        private final Queue<String> clickBuffer = new ConcurrentLinkedQueue<>();
        private static final long MAX_BUFFER_TIME_MS = 200; // Keep clicks in buffer for 200ms
        private static final double TIMING_MULTIPLIER = 0.9; // 90% of average timing
        private long averageClickInterval = 200; // Start with 200ms assumption

        public CastingSession(Player player) {
            this.player = player;
        }

        /**
         * Adds a click (either "L" or "R") to the current combo.
         * Uses a short click lock (approx 2 ticks ~100ms) to filter out repeated
         * events when a key is held down.
         */
        public void addClick(String click) {
            clickBuffer.add(click);
            comboExecutor.submit(() -> {
                try {
                    // Process buffer after short delay
                    Thread.sleep(10); // 10ms buffer aggregation
                    processBuffer();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        private void processBuffer() {
            List<String> processedClicks = new ArrayList<>();
            Instant cutoff = Instant.now().minusMillis(MAX_BUFFER_TIME_MS);
            
            while (!clickBuffer.isEmpty()) {
                processedClicks.add(clickBuffer.poll());
            }
            
            if (!processedClicks.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (String click : processedClicks) {
                        handleSingleClick(click);
                    }
                });
            }
        }

        private void handleSingleClick(String click) {
            clicks.add(click);
            lastClickTime = Instant.now();
            
            // Check if we have a valid combo
            if (clicks.size() == 3) {
                String combo = String.join("", clicks);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processCombo(player, combo);
                    resetClicks();
                });
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                clickLock = true;
                playSound(player, clickSound, (float) clickSoundVolume, (float) clickSoundPitch);
                player.sendActionBar(translateHexColors("&eCombo: &b" + String.join(",", clicks)));
            });
            
            // Start timeout check
            comboExecutor.submit(() -> {
                try {
                    Thread.sleep(comboTimeoutSeconds * 1000);
                    if (Instant.now().isAfter(lastClickTime.plusSeconds(comboTimeoutSeconds))) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendActionBar(translateHexColors(cancelMessage));
                            playSound(player, cancelSound, 1.0f, 1.0f);
                            resetClicks();
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        /**
         * Returns the number of clicks recorded.
         */
        public int getComboSize() {
            return clicks.size();
        }

        /**
         * Returns the combo as a concatenated string without commas.
         */
        public String getComboString() {
            StringBuilder sb = new StringBuilder();
            // Create a copy of the combo list to prevent ConcurrentModificationException
            List<String> currentCombo = new ArrayList<>(clicks);
            for (String click : currentCombo) {
                sb.append(click);
            }
            return sb.toString();
        }

        /**
         * Starts a timeout task that will cancel the casting session if the player
         * does not complete the three-click combo within the specified timeout.
         */
        public void startTimeoutTask(int timeoutSeconds) {
            taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                resetClicks();
                activeSessions.remove(player.getUniqueId());
                plugin.getLogger().info("Casting combo timeout for player: " + player.getName());
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound, 1.0f, 1.0f);
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

        /**
         * Resets the click sequence while keeping the casting session active
         */
        public void resetClicks() {
            player.sendActionBar(""); // Immediately clear combo display
            clicks.clear();
        }

        /**
         * Starts the cooldown period between combos
         */
        public void startCooldown() {
            cooldownEnd = Instant.now().plusMillis(comboCooldownMillis);
        }

        private void updateTiming(long newInterval) {
            averageClickInterval = (long) (averageClickInterval * 0.7 + newInterval * 0.3);
        }

        private boolean withinTimingWindow() {
            long timeSinceLast = Duration.between(lastClickTime, Instant.now()).toMillis();
            return timeSinceLast < averageClickInterval * 1.5;
        }

        public Instant getLastClickTime() {
            return lastClickTime;
        }
    }

    /**
     * Reloads the casting configuration from classes.yml.
     * Call this method on /reload so the casting system picks up configuration changes.
     */
    public void reloadCastingConfiguration() {
        comboExecutor.submit(() -> {
            loadConfiguration();
            plugin.getLogger().info("Casting config cache invalidated and reloaded");
        });
    }

    private long getConfigHash(File file) throws Exception {
        if (!file.exists()) return 0;
        String input = file.lastModified() + "-" + file.length();
        MessageDigest md = MessageDigest.getInstance("MD5");
        return ByteBuffer.wrap(md.digest(input.getBytes())).getLong();
    }

    private static class CastingConfig {
        final int timeout;
        final long cooldownMillis;
        final int leftClickLock;
        final int rightClickLock;
        final Map<String, Map<String, String>> comboMappings;
        final String activationSound;
        final String cancelSound;
        final String successSound;
        final String clickSound;
        final double clickSoundVolume;
        final double clickSoundPitch;
        final String activationMessage;
        final String cancelMessage;
        final String successMessage;
        
        CastingConfig(YamlConfiguration config) {
            this.timeout = config.getInt("casting.timeout", 5);
            this.cooldownMillis = config.getLong("casting.cooldownMillis", 1000);
            this.leftClickLock = config.getInt("casting.leftClickLock", 1);
            this.rightClickLock = config.getInt("casting.rightClickLock", 4);
            this.comboMappings = loadComboMappings(config);
            this.activationSound = config.getString("casting.activationSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            this.cancelSound = config.getString("casting.cancelSound", "ENTITY_BLAZE_HURT");
            this.successSound = config.getString("casting.successSound", "ENTITY_PLAYER_LEVELUP");
            this.clickSound = config.getString("casting.clickSound", "UI_BUTTON_CLICK");
            this.clickSoundVolume = config.getDouble("casting.clickSoundVolume", 1.0);
            this.clickSoundPitch = config.getDouble("casting.clickSoundPitch", 1.0);
            this.activationMessage = config.getString("casting.activationMessage", "&aCasting Mode Activated!");
            this.cancelMessage = config.getString("casting.cancelMessage", "&cCasting Cancelled!");
            this.successMessage = config.getString("casting.successMessage", "&bSkill Cast Successful!");
        }

        private Map<String, Map<String, String>> loadComboMappings(YamlConfiguration config) {
            Map<String, Map<String, String>> mappings = new HashMap<>();
            if (config.contains("casting.comboMappings")) {
                for (String className : config.getConfigurationSection("casting.comboMappings").getKeys(false)) {
                    Map<String, String> mapping = new HashMap<>();
                    for (String combo : config.getConfigurationSection("casting.comboMappings." + className).getKeys(false)) {
                        String skillName = config.getString("casting.comboMappings." + className + "." + combo);
                        mapping.put(combo, skillName);
                    }
                    mappings.put(className, mapping);
                }
            }
            return mappings;
        }
    }
} 