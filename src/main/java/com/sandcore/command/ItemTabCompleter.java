package com.sandcore.command;

import java.util.ArrayList;
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
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            return Collections.singletonList("give");
        }
        else if (args.length == 3) { // Item IDs
            return StringUtil.copyPartialMatches(args[2], itemsManager.getItemIds(), new ArrayList<>());
        }
        return Collections.emptyList();
    }
} 