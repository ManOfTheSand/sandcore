package com.sandcore.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.sandcore.SandCore;
import com.sandcore.items.CustomItem;
import com.sandcore.items.ItemsManager;

public class ItemCommandExecutor implements CommandExecutor {
    private final SandCore plugin;
    private final ItemsManager itemsManager;

    public ItemCommandExecutor(SandCore plugin, ItemsManager itemsManager) {
        this.plugin = plugin;
        this.itemsManager = itemsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sandcore.command.item.give")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /sandcore item give <player> <itemID> [amount]");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            handleGiveCommand(sender, args);
            return true;
        }

        sender.sendMessage("§cUnknown subcommand!");
        return false;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        String itemId = args[2];
        CustomItem customItem = itemsManager.getItem(itemId);
        if (customItem == null) {
            sender.sendMessage("§cInvalid item ID!");
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount!");
                return;
            }
        }

        ItemStack item = customItem.buildItem();
        item.setAmount(amount);
        target.getInventory().addItem(item);

        sender.sendMessage("§aGiven " + amount + " " + customItem.getDisplayName() + " §ato " + target.getName());
        target.sendMessage("§aYou received " + amount + " " + customItem.getDisplayName());
        
        plugin.getLogger().info(sender.getName() + " gave " + amount + "x" + itemId + " to " + target.getName());
    }
} 