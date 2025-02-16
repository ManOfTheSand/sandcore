package com.sandcore.command;

import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.sandcore.SandCore;
import com.sandcore.items.ItemsManager;

public class MainCommandExecutor implements CommandExecutor {

    private final SandCore plugin;
    private final ItemsManager itemsManager;
    private final ItemCommandExecutor itemCommandExecutor;

    public MainCommandExecutor(SandCore plugin, ItemsManager itemsManager) {
        this.plugin = plugin;
        this.itemsManager = itemsManager;
        this.itemCommandExecutor = new ItemCommandExecutor(plugin, itemsManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help menu
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                handleReload(sender);
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

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("sandcore.command.reload")) {
            sender.sendMessage("§cYou don't have permission to reload!");
            return;
        }

        try {
            // Reload items first
            itemsManager.reloadItems();
            
            // Then reload other components
            plugin.reloadConfig();
            plugin.getLevelManager().loadConfig(plugin.getConfig());
            plugin.getClassManager().reloadClasses();
            
            sender.sendMessage("§aConfigurations reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cReload failed! Check console for errors.");
            plugin.getLogger().severe("Reload error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6SandCore Commands:");
        sender.sendMessage("§a/sandcore reload §7- Reload plugin configs");
        sender.sendMessage("§a/sandcore item give <player> <item> [amount] §7- Give custom items");
    }
} 