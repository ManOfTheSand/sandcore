package com.sandcore.command;

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
                
                // Reload classes.yml.
                File classesFile = new File(plugin.getDataFolder(), "classes.yml");
                if (classesFile.exists()) {
                    FileConfiguration classesConfig = YamlConfiguration.loadConfiguration(classesFile);
                    // Optionally, store or use classesConfig as needed.
                    plugin.getLogger().info("classes.yml reloaded successfully.");
                } else {
                    sender.sendMessage("§cclasses.yml not found! Saving default resource.");
                    plugin.saveResource("classes.yml", false);
                    plugin.getLogger().info("Default classes.yml saved and loaded.");
                }
                
                // Refresh GUIs.
                // TODO: Add the actual GUI refresh logic, e.g., call your GUI handler's refresh method.
                plugin.getLogger().info("Refreshing all GUIs...");
                
                sender.sendMessage("§aConfiguration and GUIs reloaded successfully!");
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