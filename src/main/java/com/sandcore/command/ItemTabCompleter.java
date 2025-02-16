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
        if (itemsManager == null) return Collections.emptyList();
        
        if (args.length == 1) {
            return Collections.singletonList("give");
        }
        else if (args.length == 3) { // Item IDs
            List<String> ids = itemsManager.getItemIds() != null ? 
                itemsManager.getItemIds() : 
                Collections.emptyList();
            return StringUtil.copyPartialMatches(args[2], ids, new ArrayList<>());
        }
        return Collections.emptyList();
    }
} 