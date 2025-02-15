package com.sandcore;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.classes.ClassManager;
import com.sandcore.command.ClassesCommandExecutor;
import com.sandcore.command.MainCommandExecutor;

public class SandCore extends JavaPlugin {

    // Keep a reference so that the ClassManager can be used later in your plugin.
    private ClassManager classManager;

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

        getLogger().info("SandCore enabled successfully!");
    }

    @Override
    public void onDisable() {
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
                java.io.File classesFile = new java.io.File(getDataFolder(), "classes.yml");
                if (!classesFile.exists()) {
                    getLogger().info("classes.yml not found, saving default from resources...");
                    saveResource("classes.yml", false);
                }
                // Load the classes.yml using YamlConfiguration.
                org.bukkit.configuration.file.FileConfiguration classesConfig =
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(classesFile);
                getLogger().info("classes.yml loaded successfully.");

                // Schedule any sync-based operations (e.g., GUI refresh) on the main thread.
                Bukkit.getScheduler().runTask(SandCore.this, () -> {
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
}
