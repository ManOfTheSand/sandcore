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

public class MainTabCompleter implements TabCompleter {
    private final ItemsManager itemsManager;
    private final List<String> mainSubcommands = Arrays.asList("reload", "item", "help");
    private final List<String> itemSubcommands = Collections.singletonList("give");

    public MainTabCompleter(ItemsManager itemsManager) {
        this.itemsManager = itemsManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // /sandcore [subcommand]
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], mainSubcommands, new ArrayList<>());
        }
        
        String subCommand = args[0].toLowerCase();
        
        // Handle 'item' subcommand
        if (subCommand.equals("item")) {
            // /sandcore item [give]
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], itemSubcommands, new ArrayList<>());
            }
            
            // /sandcore item give [player] [itemID] [amount]
            if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
                return handleItemGiveCompletion(sender, args);
            }
        }
        
        // Handle 'reload' - no arguments needed
        if (subCommand.equals("reload") && args.length > 1) {
            return Collections.emptyList();
        }

        return completions;
    }

    private List<String> handleItemGiveCompletion(CommandSender sender, String[] args) {
        if (args.length == 3) { // Player names
            return null; // Bukkit handles online players
        }
        else if (args.length == 4) { // Item IDs
            if (itemsManager == null) return Collections.emptyList();
            List<String> ids = itemsManager.getItemIds();
            return StringUtil.copyPartialMatches(args[3], ids, new ArrayList<>());
        }
        else if (args.length == 5) { // Amount suggestions
            return Arrays.asList("1", "8", "16", "32", "64");
        }
        return Collections.emptyList();
    }
} 