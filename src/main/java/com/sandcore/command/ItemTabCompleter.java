package com.sandcore.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import com.sandcore.items.ItemsManager;

public class ItemTabCompleter implements TabCompleter {
    private final ItemsManager itemsManager;

    public ItemTabCompleter(ItemsManager itemsManager) {
        this.itemsManager = itemsManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (itemsManager == null) return Collections.emptyList();
        
        // sandcore item give <player> <item> [amount]
        if (args.length >= 1 && args[0].equalsIgnoreCase("item")) {
            if (args.length == 1) {
                return Collections.singletonList("give");
            }
            else if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
                if (args.length == 2) {
                    return Collections.singletonList("give");
                }
                else if (args.length == 3) { // Player names
                    return null; // Let Bukkit handle player name completion
                }
                else if (args.length == 4) { // Item IDs
                    List<String> ids = itemsManager.getItemIds();
                    return StringUtil.copyPartialMatches(args[3], ids, new ArrayList<>());
                }
                else if (args.length == 5) { // Amount suggestions
                    return Arrays.asList("1", "8", "16", "32", "64");
                }
            }
        }
        return Collections.emptyList();
    }
} 