package com.sandcore;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.classes.ClassManager;
import com.sandcore.command.ClassInfoCommandExecutor;
import com.sandcore.command.ClassesCommandExecutor;
import com.sandcore.command.DebugXPCommandExecutor;
import com.sandcore.command.GiveXPCommandExecutor;
import com.sandcore.command.MainCommandExecutor;
import com.sandcore.command.MainTabCompleter;
import com.sandcore.command.ProfileCommandExecutor;
import com.sandcore.command.SetLevelCommandExecutor;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.hud.HUDManager;
import com.sandcore.items.ItemsManager;
import com.sandcore.levels.LevelManager;
import com.sandcore.levels.XPSourceManager;
import com.sandcore.listeners.XPListener;

public class SandCore extends JavaPlugin {

    // Keep a reference so that the ClassManager can be used later in your plugin.
    private ClassManager classManager;
    private LevelManager levelManager;
    private XPSourceManager xpSourceManager;
    private PlayerDataManager playerDataManager;
    private HUDManager hudManager;
    private ItemsManager itemsManager;

    @Override
    public void onEnable() {
        getLogger().info("SandCore enabling...");

        // Display an eye-catching startup message with ASCII art and version info.
        printStartupMessage();

        // Load configuration files asynchronously.
        loadConfigurations();

        // Register core services.
        registerServices();

        // Leveling system initialization:
        saveDefaultConfig();
        levelManager = new LevelManager(getLogger());
        levelManager.loadConfiguration(getConfig());
        
        File xpSourcesFile = new File(getDataFolder(), "xp-sources.yml");
        if (!xpSourcesFile.exists()) {
            saveResource("xp-sources.yml", false);
        }
        xpSourceManager = new XPSourceManager(getLogger());
        xpSourceManager.loadXPSources(xpSourcesFile);
        
        playerDataManager = new PlayerDataManager(getDataFolder(), getLogger());
        hudManager = new HUDManager(getLogger());
        
        // Initialize the ClassManager (loads classes from classes.yml).
        classManager = new ClassManager(this);

        // Now that leveling system dependencies are initialized, register the commands.
        registerCommands();

        // Register global event listeners.
        registerEventListeners();

        // Register the XP listener for awarding XP on mob kills.
        getServer().getPluginManager().registerEvents(
                new XPListener(xpSourceManager, playerDataManager, levelManager, hudManager, getLogger()), this);

        // Schedule an asynchronous repeating task to auto-save player data every 60 seconds.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataManager != null) {
                playerDataManager.savePlayerData();
                getLogger().info("Auto-saved player data.");
            }
        }, 1200L, 1200L); // 1200 ticks = 60 seconds at 20 tps

        // Schedule a synchronous task to update XP bars every tick for smoother updates.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                // Update the XP bar from the plugin's internal data.
                var data = playerDataManager.getPlayerData(player.getUniqueId());
                int currentLevel = data.getLevel();
                int xpForCurrent = levelManager.getXPForLevel(currentLevel);
                int xpForNext = levelManager.getXPForLevel(currentLevel + 1);
                float progress = 0f;
                if (xpForNext - xpForCurrent > 0) {
                    progress = (float)(data.getXP() - xpForCurrent) / (xpForNext - xpForCurrent);
                }
                player.setExp(progress);
                player.setLevel(currentLevel);
            });
        }, 1L, 1L);

        // Initialize itemsManager
        this.itemsManager = new ItemsManager(this);
        
        // Register commands
        MainCommandExecutor mainExecutor = new MainCommandExecutor(this);
        getCommand("sandcore").setExecutor(mainExecutor);
        getCommand("sandcore").setTabCompleter(new MainTabCompleter(itemsManager));

        getLogger().info("SandCore enabled successfully with enhanced leveling system!");
    }

    @Override
    public void onDisable() {
        // Save player data on plugin disable.
        if (playerDataManager != null) {
            playerDataManager.savePlayerData();
        }
        getLogger().info("SandCore disabled successfully!");
    }

    public void reloadXPSources() {
        File xpSourcesFile = new File(getDataFolder(), "xp-sources.yml");
        if (!xpSourcesFile.exists()) {
            saveResource("xp-sources.yml", false);
        }
        xpSourceManager.loadXPSources(xpSourcesFile);
        getLogger().info("xp-sources.yml reloaded.");
    }

    private void loadConfigurations() {
        getLogger().info("Starting asynchronous configuration loading...");
        // Run the configuration loading on a separate thread to keep the main thread responsive.
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // Load config.yml (automatically copied to the data folder if not present)
                saveDefaultConfig();
                getLogger().info("config.yml loaded successfully.");
                
                // Load classes.yml from the plugin's data folder.
                // If not found, copy the default from the jar.
                File classesFile = new File(getDataFolder(), "classes.yml");
                if (!classesFile.exists()) {
                    getLogger().info("classes.yml not found, saving default from resources...");
                    saveResource("classes.yml", false);
                }
                YamlConfiguration.loadConfiguration(classesFile);
                getLogger().info("classes.yml loaded successfully.");
                
                // NEW: Load gui.yml from the plugin's data folder.
                // If not found, copy the default from the jar.
                File guiFile = new File(getDataFolder(), "gui.yml");
                if (!guiFile.exists()) {
                    getLogger().info("gui.yml not found, saving default from resources...");
                    saveResource("gui.yml", false);
                }
                getLogger().info("gui.yml loaded successfully.");
                
                // Schedule any sync-based operations (e.g., GUI refresh) on the main thread.
                Bukkit.getScheduler().runTask(this, () -> {
                    // TODO: Add any post-configuration sync operations here (e.g., refresh GUIs).
                    getLogger().info("Post-configuration sync operations completed.");
                });
            } catch (Exception e) {
                getLogger().severe("Error loading configuration: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void printStartupMessage() {
        String version = getDescription().getVersion();
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
          .append("**********************************\n")
          .append("*        SandCore v").append(version).append("        *\n")
          .append("*    Minecraft MMORPG Plugin     *\n")
          .append("**********************************\n")
          .append("Modules Loaded:\n")
          .append(" - StatsManager\n")
          .append(" - ClassManager\n")
          .append(" - GUI Handler\n")
          .append(" - Command Executor\n");
        getLogger().info(sb.toString());
    }

    private void registerServices() {
        try {
            getLogger().info("Registering core services...");
            // For demonstration purposes, we simply log the registration of each service.
            getLogger().info("Registering StatsManager...");
            // ServiceRegistry.register(new StatsManager());
            getLogger().info("StatsManager registered successfully!");

            getLogger().info("Registering ClassManager...");
            // ServiceRegistry.register(new ClassManager());
            getLogger().info("ClassManager registered successfully!");

            getLogger().info("Registering GUI Handler...");
            // ServiceRegistry.register(new GUIHandler());
            getLogger().info("GUI Handler registered successfully!");
        } catch (Exception e) {
            getLogger().severe("Error registering services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        try {
            getLogger().info("Registering commands...");
            // Register reload command.
            getCommand("reload").setExecutor(new MainCommandExecutor(this));
            
            // Existing commands
            if (getCommand("classes") != null) {
                getCommand("classes").setExecutor(new ClassesCommandExecutor(this, classManager));
                getLogger().info("Command /classes registered successfully!");
            } else {
                getLogger().severe("Command /classes is not defined in plugin.yml!");
            }
            if (getCommand("classinfo") != null) {
                getCommand("classinfo").setExecutor(new ClassInfoCommandExecutor(this, classManager));
                getLogger().info("Command /classinfo registered successfully!");
            } else {
                getLogger().severe("Command /classinfo is not defined in plugin.yml!");
            }

            // Register leveling system admin commands.
            if(getCommand("givexp") != null) {
                getCommand("givexp").setExecutor(new GiveXPCommandExecutor(this, levelManager, playerDataManager, hudManager));
                getLogger().info("Command /givexp registered successfully.");
            } else {
                getLogger().severe("Command /givexp is not defined in plugin.yml!");
            }
            if(getCommand("setlevel") != null) {
                getCommand("setlevel").setExecutor(new SetLevelCommandExecutor(this, levelManager, playerDataManager, hudManager));
                getLogger().info("Command /setlevel registered successfully.");
            } else {
                getLogger().severe("Command /setlevel is not defined in plugin.yml!");
            }
            if(getCommand("debugxp") != null) {
                getCommand("debugxp").setExecutor(new DebugXPCommandExecutor(this, levelManager, playerDataManager, hudManager));
                getLogger().info("Command /debugxp registered successfully.");
            } else {
                getLogger().severe("Command /debugxp is not defined in plugin.yml!");
            }
            if(getCommand("profile") != null) {
                getCommand("profile").setExecutor(new ProfileCommandExecutor(this, playerDataManager));
                getLogger().info("Command /profile registered successfully.");
            } else {
                getLogger().severe("Command /profile is not defined in plugin.yml!");
            }
        } catch (Exception e) {
            getLogger().severe("Error registering commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerEventListeners() {
        try {
            getLogger().info("Registering event listeners...");
            // Register the Profile GUI listener.
            getServer().getPluginManager().registerEvents(new com.sandcore.listeners.ProfileGUIListener(), this);
            // Register the Class Selection listener.
            getServer().getPluginManager().registerEvents(new com.sandcore.listeners.ClassSelectionListener(this, playerDataManager), this);
            // Register Vanilla XP listener to prevent vanilla XP from affecting the XP bar.
            getServer().getPluginManager().registerEvents(new com.sandcore.listeners.VanillaXPListener(playerDataManager, levelManager), this);
            getLogger().info("Global event listeners registered successfully!");
        } catch (Exception e) {
            getLogger().severe("Error registering event listeners: " + e.getMessage());
            e.printStackTrace();
        }
        // Instantiate the CastingSystem to register its event listeners.
        new com.sandcore.casting.CastingSystem(this);
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    // Newly added getter method for PlayerDataManager.
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ItemsManager getItemsManager() {
        return itemsManager;
    }
}
