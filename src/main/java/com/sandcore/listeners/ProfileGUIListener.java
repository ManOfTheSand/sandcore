package com.sandcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class ProfileGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if(event.getInventory().getHolder() instanceof com.sandcore.gui.ProfileGUIHolder) {
            event.setCancelled(true);
        }
    }
} 