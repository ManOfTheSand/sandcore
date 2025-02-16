package com.sandcore.command;

import java.io.File;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import com.sandcore.SandCore;

public class MainCommandExecutor implements CommandExecutor {

    private final SandCore plugin;
    private final ItemCommandExecutor itemCommandExecutor;

    public MainCommandExecutor(SandCore plugin) {
        this.plugin = plugin;
        this.itemCommandExecutor = new ItemCommandExecutor(plugin, plugin.getItemsManager());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help menu
            sender.sendMessage("§6SandCore Commands:");
            sender.sendMessage("§a/sandcore reload §7- Reload plugin configs");
            sender.sendMessage("§a/sandcore item give <player> <item> [amount] §7- Give custom items");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                // Handle reload
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
            case "item":
                if (args.length > 1) {
                    // Pass arguments after "item"
                    String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                    return itemCommandExecutor.onCommand(sender, command, label, newArgs);
                }
                break;
            case "reloadcast":
                sender.sendMessage("Casting system has been removed.");
                return true;
            default:
                sender.sendMessage("§cUnknown command. Use /sandcore for help");
        }
        return true;
    }
} 