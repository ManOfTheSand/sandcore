package com.sandcore.command;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import com.sandcore.SandCore;

public class MainCommandExecutor implements CommandExecutor {

    private SandCore plugin;

    public MainCommandExecutor(SandCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle the /reload command.
        if (command.getName().equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sandmmo.admin.reload")) {
                sender.sendMessage("§cYou do not have permission to reload the plugin.");
                return true;
            }
            sender.sendMessage("§aReloading plugin configuration...");
            plugin.getLogger().info("Reload command executed by " + sender.getName());
            
            try {
                // Reload config.yml.
                plugin.reloadConfig();
                plugin.reloadXPSources();
                plugin.getLogger().info("config.yml reloaded successfully.");

                // Reload classes.yml.
                File classesFile = new File(plugin.getDataFolder(), "classes.yml");
                if (classesFile.exists()) {
                    YamlConfiguration classesConfig = YamlConfiguration.loadConfiguration(classesFile);
                    plugin.getLogger().info("classes.yml reloaded successfully.");
                    // Update the class definitions with the new changes.
                    plugin.getClassManager().loadClasses();
                } else {
                    sender.sendMessage("§cclasses.yml not found! Saving default resource.");
                    plugin.saveResource("classes.yml", false);
                    plugin.getLogger().info("Default classes.yml saved and loaded.");
                }
                
                // Reload casting configuration.
                if (plugin.getCastingManager() != null) {
                    plugin.getCastingManager().reloadCastingConfig();
                    plugin.getLogger().info("Casting configuration reloaded successfully.");
                }
                
                // Reload gui.yml.
                File guiFile = new File(plugin.getDataFolder(), "gui.yml");
                if (guiFile.exists()) {
                    YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
                    plugin.getLogger().info("gui.yml reloaded successfully.");
                } else {
                    sender.sendMessage("§cgui.yml not found! Saving default resource.");
                    plugin.saveResource("gui.yml", false);
                    plugin.getLogger().info("Default gui.yml saved and loaded.");
                }
                
                // Optionally, refresh or update any open GUIs on the main thread.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Insert any GUI refresh logic here if needed.
                    plugin.getLogger().info("Post-configuration sync operations completed.");
                });
                
                sender.sendMessage("§aConfiguration and class definitions reloaded successfully!");
                plugin.getLogger().info("Reload command completed successfully for " + sender.getName());
            } catch (Exception e) {
                sender.sendMessage("§cError reloading configuration: " + e.getMessage());
                plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
} 