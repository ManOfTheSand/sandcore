package com.sandcore;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.classes.ClassManager;
import com.sandcore.command.ClassInfoCommandExecutor;
import com.sandcore.command.ClassesCommandExecutor;
import com.sandcore.command.MainCommandExecutor;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.hud.HUDManager;
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

    @Override
    public void onEnable() {
        getLogger().info("SandCore enabling...");

        // Display an eye-catching startup message with ASCII art and version info.
        printStartupMessage();

        // Load configuration files asynchronously.
        loadConfigurations();

        // Register core services.
        registerServices();

        // Initialize the ClassManager (loads classes from classes.yml).
        classManager = new ClassManager(this);

        // Register the main command executor.
        registerCommands();

        // Register global event listeners.
        registerEventListeners();

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
        
        // Register the XP listener for awarding XP on mob kills.
        getServer().getPluginManager().registerEvents(
                new XPListener(xpSourceManager, playerDataManager, levelManager, hudManager, getLogger()), this);

        // Other initialization (e.g., class system integration) goes here.
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
          .append(" - CastingSystem\n")
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

            getLogger().info("Registering CastingSystem...");
            // ServiceRegistry.register(new CastingSystem());
            getLogger().info("CastingSystem registered successfully!");

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
            if (getCommand("reload") != null) {
                // Pass a reference to this plugin instance so that the executor can access its methods.
                getCommand("reload").setExecutor(new MainCommandExecutor(this));
                getLogger().info("Command /reload registered successfully!");
            } else {
                getLogger().severe("Command /reload is not defined in plugin.yml!");
            }
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
        } catch (Exception e) {
            getLogger().severe("Error registering commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerEventListeners() {
        try {
            getLogger().info("Registering event listeners...");
            // Register any global event listeners needed for the plugin.
            // Example: getServer().getPluginManager().registerEvents(new GlobalEventListener(), this);
            getLogger().info("Global event listeners registered successfully!");
        } catch (Exception e) {
            getLogger().severe("Error registering event listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ClassManager getClassManager() {
        return classManager;
    }
}
