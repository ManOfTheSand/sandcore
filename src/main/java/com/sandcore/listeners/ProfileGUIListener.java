package com.sandcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class ProfileGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        // Check if the inventory title (stripped of color codes) equals "Profile"
        String title = ChatColor.stripColor(view.getTitle());
        if(title.equalsIgnoreCase("Profile")) {
            event.setCancelled(true);
        }
    }
} 