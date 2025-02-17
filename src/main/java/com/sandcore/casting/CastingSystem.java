package com.sandcore.casting;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import com.sandcore.SandCore;
import com.sandcore.data.PlayerDataManager;

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
    private final PlayerDataManager playerDataManager;
    private Location loc;
    // Configurable options for casting
    private int comboTimeoutSeconds;
    private long comboCooldownMillis;
    private int leftClickLockTicks;
    private int rightClickLockTicks;
    private String activationSound;
    private String cancelSound;
    private String successSound;
    private String clickSound;
    private double clickSoundVolume;
    private double clickSoundPitch;
    private String activationMessage;
    private String cancelMessage;
    private String successMessage;
    // Mapping: Player's class => combo pattern (e.g., "L,R,L") => MythicMob skill name.
    private Map<String, Map<String, String>> comboMappings;
    // Active casting sessions keyed by player UUID.
    private final Map<UUID, CastingSession> activeSessions = new ConcurrentHashMap<>();
    private final ExecutorService comboExecutor = Executors.newCachedThreadPool();
    private CastingConfig cachedConfig;
    private long lastConfigHash;
    // Cooldown tracking for mode toggling (1 second)
    private final Map<UUID, Instant> toggleCooldowns = new ConcurrentHashMap<>();
    // Add this field to track last click times
    private final Map<UUID, Long> lastClickTimes = new HashMap<>();
    private final Map<UUID, List<String>> clickSequences = new HashMap<>();
    private final Map<UUID, Instant> lastCastTimes = new HashMap<>();
    private final Map<UUID, Instant> lastActivationTimes = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> activeCasters = new ConcurrentHashMap<>();

    /**
     * Constructor. Loads the casting configuration from classes.yml
     * and registers this system as an event listener.
     */
    public CastingSystem(SandCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
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
                cachedConfig = new CastingConfig(config, plugin);
                lastConfigHash = currentHash;
                plugin.getLogger().info("Reloaded and cached casting configuration");
            }
            
            // Update runtime values from cache
            comboTimeoutSeconds = cachedConfig.timeout;
            comboCooldownMillis = cachedConfig.comboCooldownMillis;
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
        // Only handle right-click air/block events for activation
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Debug: Log initial interaction attempt
        plugin.getLogger().info("[DEBUG] Casting mode toggle attempt by " + player.getName() + 
                              " | Holding items: " + !player.getInventory().getItemInMainHand().getType().isAir() +
                              " | In casting mode: " + activeCasters.containsKey(playerId));

        // Check if player is holding any items
        if (!player.getInventory().getItemInMainHand().getType().isAir() ||
            !player.getInventory().getItemInOffHand().getType().isAir()) {
            plugin.getLogger().info("[DEBUG] Blocked casting toggle - holding items");
            return;
        }

        // Check cooldown with debug info
        Instant lastActivation = lastActivationTimes.getOrDefault(playerId, Instant.MIN);
        Duration timeSinceLast = Duration.between(lastActivation, Instant.now());
        boolean onCooldown = timeSinceLast.toMillis() < comboCooldownMillis;
        
        plugin.getLogger().info("[DEBUG] Cooldown check for " + player.getName() + 
                              " | Last activation: " + lastActivation +
                              " | Elapsed: " + timeSinceLast.toMillis() + "ms" +
                              " | Cooldown: " + comboCooldownMillis + "ms" +
                              " | On cooldown: " + onCooldown);

        if (onCooldown) {
            long remaining = comboCooldownMillis - timeSinceLast.toMillis();
            player.sendActionBar("§cYou must wait " + (remaining/1000) + "s before toggling again");
            plugin.getLogger().info("[COOLDOWN] " + player.getName() + " tried to toggle too soon");
            return;
        }

        // Toggle casting mode with debug logging
        if (activeCasters.containsKey(playerId)) {
            plugin.getLogger().info("[DEBUG] Deactivating casting mode for " + player.getName());
            deactivateCastingMode(player);
        } else {
            plugin.getLogger().info("[DEBUG] Activating casting mode for " + player.getName());
            activateCastingMode(player);
        }
        
        event.setCancelled(true); // Only cancel if we handled the toggle
    }

    /**
     * Activates casting mode for the specified player.
     * Displays an action bar message and plays the activation sound, then starts the timeout.
     */
    private void activateCastingMode(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (activeCasters.containsKey(playerId)) {
            plugin.getLogger().warning("[DUPLICATE] Tried to activate casting mode for " + player.getName());
            return;
        }
        
        activeCasters.put(playerId, new ArrayList<>());
        // Start cooldown timer only when DEactivating
        activeSessions.put(playerId, new CastingSession(player));
        
        // Debug: Log activation details
        plugin.getLogger().info("[ACTIVATION] " + player.getName() + 
                               " | Selected class: " + playerDataManager.getPlayerData(playerId).getSelectedClass() +
                               " | Combo mappings: " + comboMappings);
        
        // Visual/audio feedback
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendActionBar(activationMessage);
            playSound(player, activationSound, 1.0f, 1.0f);
        });
    }

    /**
     * Processes the three-click combo: determines the player's class, looks up the corresponding
     * MythicMob skill mapping, and attempts to cast the skill.
     */
    private void processCombo(Player player, String combo) {
        boolean castSuccess = false;
        String playerClass = plugin.getClassManager().getPlayerClass(player.getUniqueId());
        
        // Single declaration here (remove any others)
        CastingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        try {
            // Retrieve the player's selected class from the player's data.
            String selectedClass = playerDataManager.getPlayerData(player.getUniqueId()).getSelectedClass();
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
                    activeSessions.get(player.getUniqueId()).startCooldown(false);
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
            castSuccess = castMythicMobSkill(player, skillName);
            if (castSuccess) {
                session.resetClicks();
                session.restartTimeout();
                session.markComboUsed();
                
                long formattedTime = Duration.between(session.getLastClickTime(), Instant.now()).toMillis();
                player.sendTitle("", translateHexColors("&a&l" + combo + " &r&7(" + formattedTime + "ms)"), 5, 20, 5);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getEyeLocation(), 5, 0.2, 0.5, 0.2, 0.1);
            } else {
                session.resetClicks();
                session.restartTimeout();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendActionBar(translateHexColors(cancelMessage));
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error casting skill: " + e.getMessage());
            e.printStackTrace();
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
        String useSound = soundName != null && !soundName.isEmpty() ? soundName : "block.note_block.harp";
        
        try {
            Sound sound = Sound.valueOf(useSound.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Falling back to default sound. Invalid sound: " + useSound);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, volume, pitch);
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
        private long lastComboTime = 0;  // Track last combo time

        public CastingSession(Player player) {
            this.player = player;
        }

        /**
         * Adds a click (either "L" or "R") to the current combo.
         * Uses a short click lock (approx 2 ticks ~100ms) to filter out repeated
         * events when a key is held down.
         */
        public void addClick(String click) {
            if (Instant.now().isBefore(cooldownEnd)) {
                return; // Respect cooldown period
            }
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
        public void startCooldown(boolean success) {
            // Empty implementation since we're removing cooldowns
        }

        public boolean isOnCooldown() {
            long elapsed = System.currentTimeMillis() - lastComboTime;
            return elapsed < comboCooldownMillis;
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

        public void restartTimeout() {
            cancelTimeout();
            
            taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!activeSessions.containsKey(player.getUniqueId())) return;
                
                plugin.getLogger().info("Casting combo timeout for player: " + player.getName());
                resetClicks();
                activeSessions.remove(player.getUniqueId());
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound, 1.0f, 1.0f);
            }, comboTimeoutSeconds * 20L).getTaskId();
        }

        public void markComboUsed() {
            lastComboTime = System.currentTimeMillis();
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
        try {
            String input = file.lastModified() + "-" + file.length();
            MessageDigest md = MessageDigest.getInstance("MD5");
            return ByteBuffer.wrap(md.digest(input.getBytes())).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    public void reloadConfig() {
        File configFile = new File(plugin.getDataFolder(), "classes.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection castingSection = config.getConfigurationSection("casting");
        
        if (castingSection != null) {
            this.cachedConfig = new CastingConfig(config, plugin);
            
            try {
                this.lastConfigHash = getConfigHash(configFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Error calculating config hash: " + e.getMessage());
                this.lastConfigHash = -1; // Fallback value
            }
            
            // Update runtime values from new config
            this.comboTimeoutSeconds = cachedConfig.timeout;
            this.comboCooldownMillis = cachedConfig.comboCooldownMillis;
            this.leftClickLockTicks = cachedConfig.leftClickLock;
            this.rightClickLockTicks = cachedConfig.rightClickLock;
            this.activationSound = cachedConfig.activationSound;
            this.cancelSound = cachedConfig.cancelSound;
            this.successSound = cachedConfig.successSound;
            this.clickSound = cachedConfig.clickSound;
            this.clickSoundVolume = cachedConfig.clickSoundVolume;
            this.clickSoundPitch = cachedConfig.clickSoundPitch;
            this.activationMessage = cachedConfig.activationMessage;
            this.cancelMessage = cachedConfig.cancelMessage;
            this.successMessage = cachedConfig.successMessage;
        }
    }

    private static class CastingConfig {
        final int timeout;
        final long comboCooldownMillis;
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
        
        private final SandCore plugin;
        
        CastingConfig(YamlConfiguration config, SandCore plugin) {
            this.plugin = plugin;
            ConfigurationSection castingSection = config.getConfigurationSection("casting");
            this.timeout = castingSection.getInt("timeout", 5);
            this.comboCooldownMillis = castingSection.getLong("comboCooldownMillis", 1000);
            this.leftClickLock = castingSection.getInt("leftClickLock", 1);
            this.rightClickLock = castingSection.getInt("rightClickLock", 4);
            this.comboMappings = loadComboMappings(config);
            this.activationSound = castingSection.getString("activationSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            this.cancelSound = castingSection.getString("cancelSound", "ENTITY_BLAZE_HURT");
            this.successSound = castingSection.getString("successSound", "ENTITY_PLAYER_LEVELUP");
            this.clickSound = castingSection.getString("clickSound", "UI_BUTTON_CLICK");
            this.clickSoundVolume = castingSection.getDouble("clickSoundVolume", 1.0);
            this.clickSoundPitch = castingSection.getDouble("clickSoundPitch", 1.0);
            this.activationMessage = castingSection.getString("activationMessage", "&aCasting Mode Activated!");
            this.cancelMessage = castingSection.getString("cancelMessage", "&cCasting Cancelled!");
            this.successMessage = castingSection.getString("successMessage", "&bSkill Cast Successful!");
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

        private String validateSound(String soundName, String defaultSound) {
            if (soundName == null || soundName.isEmpty()) {
                return defaultSound;
            }
            try {
                Sound.valueOf(soundName.toUpperCase());
                return soundName;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound name in config: " + soundName);
                return defaultSound;
            }
        }
    }

    public long getComboCooldownMillis() {
        return comboCooldownMillis;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (activeSessions.containsKey(uuid)) {
            event.setCancelled(true); // Prevent item swapping
            CastingSession session = activeSessions.get(uuid);
            session.cancelTimeout();
            activeSessions.remove(uuid);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendActionBar(translateHexColors(cancelMessage));
                playSound(player, cancelSound, 1.0f, 1.0f);
            });
        }
    }

    private boolean isValidSound(String soundName) {
        try {
            Sound.valueOf(soundName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void deactivateCastingMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (!activeCasters.containsKey(playerId)) return;
        
        // Start cooldown timer on DEactivation
        lastActivationTimes.put(playerId, Instant.now());
        activeCasters.remove(playerId);
        activeSessions.remove(playerId);
        
        // Debug: Log deactivation
        plugin.getLogger().info("[DEACTIVATION] " + player.getName());
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendActionBar(cancelMessage);
            playSound(player, cancelSound, 1.0f, 1.0f);
        });
    }

    // Add new debug method
    public void printDebugState(Player player) {
        UUID playerId = player.getUniqueId();
        plugin.getLogger().info("[DEBUG] Casting System State for " + player.getName() +
                              "\n- In casting mode: " + activeCasters.containsKey(playerId) +
                              "\n- Last activation: " + lastActivationTimes.get(playerId) +
                              "\n- Current combo: " + activeCasters.getOrDefault(playerId, List.of()) +
                              "\n- Cooldown remaining: " + getRemainingCooldown(playerId) + "ms");
    }

    private long getRemainingCooldown(UUID playerId) {
        Instant last = lastActivationTimes.get(playerId);
        if (last == null) return 0;
        return comboCooldownMillis - Duration.between(last, Instant.now()).toMillis();
    }
} 