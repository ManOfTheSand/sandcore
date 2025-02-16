package com.sandcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import com.sandcore.SandCore;
import com.sandcore.items.ItemsManager;

public class ItemUpdateListener implements Listener {
    private final ItemsManager itemsManager;

    public ItemUpdateListener(SandCore plugin) {
        this.itemsManager = plugin.getItemsManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;

        ItemStack updated = itemsManager.getUpdatedItem(currentItem);
        if (updated != null) {
            event.setCurrentItem(updated);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        ItemStack updated = itemsManager.getUpdatedItem(item);
        if (updated != null) {
            event.getPlayer().getInventory().setItem(event.getPlayer().getInventory().getHeldItemSlot(), updated);
        }
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        ItemStack updated = itemsManager.getUpdatedItem(item);
        
        if (updated != null) {
            event.getItem().setItemStack(updated);
        }
    }
} 