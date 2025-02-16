package com.sandcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import com.sandcore.SandCore;
import com.sandcore.items.ItemsManager;
import com.sandcore.items.CustomItem;
import com.sandcore.classes.ClassManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemUpdateListener implements Listener {
    private final ItemsManager itemsManager;
    private final ClassManager classManager;

    public ItemUpdateListener(SandCore plugin, ItemsManager itemsManager, ClassManager classManager) {
        this.itemsManager = itemsManager;
        this.classManager = classManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
            CustomItem customItem = itemsManager.getItemFromStack(updated);
            if (customItem != null && !customItem.getRequiredClasses().isEmpty()) {
                Player player = event.getPlayer();
                String playerClass = classManager.getPlayerClass(player.getUniqueId());
                
                // Update lore colors
                ItemMeta meta = updated.getItemMeta();
                List<String> lore = meta.getLore();
                if (lore != null) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : lore) {
                        if (line.startsWith(ChatColor.GRAY + "Class: ")) {
                            line = ChatColor.GRAY + "Class: " + customItem.getRequiredClasses().stream()
                                .map(cls -> classManager.getFormattedClassName(cls) + 
                                     (cls.equalsIgnoreCase(playerClass) ? 
                                      ChatColor.GREEN : ChatColor.RED))
                                .collect(Collectors.joining(ChatColor.GRAY + ", "));
                        }
                        newLore.add(line);
                    }
                    meta.setLore(newLore);
                    updated.setItemMeta(meta);
                }
                
                // Check class requirement
                if (!customItem.getRequiredClasses().contains(playerClass)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item requires: " + 
                        String.join(", ", customItem.getRequiredClasses()));
                }
            }
            
            event.getPlayer().getInventory().setItem(
                event.getPlayer().getInventory().getHeldItemSlot(), 
                updated
            );
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