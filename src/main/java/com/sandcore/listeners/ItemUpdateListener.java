package com.sandcore.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
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

        Player player = event.getPlayer();
        ItemStack updated = itemsManager.getUpdatedItem(item);
        
        if (updated != null) {
            // Run async processing for lore updates
            Bukkit.getScheduler().runTaskAsynchronously(itemsManager.getPlugin().getServer().getPluginManager().getPlugin("SandCore"), () -> {
                CustomItem customItem = itemsManager.getItemFromStack(updated);
                if (customItem != null && !customItem.getRequiredClasses().isEmpty()) {
                    String playerClass = classManager.getPlayerClass(player.getUniqueId());
                    ItemMeta meta = updated.getItemMeta();
                    List<String> lore = meta.getLore();

                    if (lore != null) {
                        List<String> newLore = processLoreAsync(lore, customItem, playerClass);
                        
                        // Schedule sync task for inventory update
                        Bukkit.getScheduler().runTask(itemsManager.getPlugin().getServer().getPluginManager().getPlugin("SandCore"), () -> {
                            meta.setLore(newLore);
                            updated.setItemMeta(meta);
                            
                            // Update class requirement check
                            if (!customItem.getRequiredClasses().contains(playerClass)) {
                                event.setCancelled(true);
                                player.sendMessage(ChatColor.RED + "This item requires: " + 
                                    String.join(", ", customItem.getRequiredClasses()));
                            }
                            
                            // Update item in hand
                            player.getInventory().setItem(
                                player.getInventory().getHeldItemSlot(), 
                                updated
                            );
                        });
                    }
                }
            });
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

    private List<String> processLoreAsync(List<String> lore, CustomItem customItem, String playerClass) {
        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            if (line.contains("{classes}")) {
                String classLine = customItem.getRequiredClasses().stream()
                    .map(cls -> {
                        String formattedName = classManager.getFormattedClassName(cls);
                        ChatColor color = cls.equalsIgnoreCase(playerClass) 
                            ? ChatColor.GREEN 
                            : ChatColor.RED;
                        // Preserve existing color context before adding class color
                        return ChatColor.getLastColors(line.split("\\{classes}")[0]) + color + formattedName;
                    })
                    .collect(Collectors.joining(ChatColor.GRAY + ", "));
                
                line = line.replace("{classes}", classLine);
            }
            newLore.add(line);
        }
        return newLore;
    }
} 