package com.sandcore.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.sandcore.SandCore;
import com.sandcore.classes.ClassManager;
import com.sandcore.items.CustomItem;
import com.sandcore.items.ItemsManager;

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
                        if (line.contains("{classes}")) {
                            // Replace {classes} with color-coded class names
                            String classLine = customItem.getRequiredClasses().stream()
                                .map(cls -> {
                                    String formattedName = classManager.getFormattedClassName(cls);
                                    ChatColor color = cls.equalsIgnoreCase(playerClass) 
                                        ? ChatColor.GREEN 
                                        : ChatColor.RED;
                                    return color + formattedName;
                                })
                                .collect(Collectors.joining(ChatColor.GRAY + ", "));
                                
                            line = line.replace("{classes}", classLine);
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