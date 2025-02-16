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
import com.sandcore.data.PlayerDataManager;
import com.sandcore.items.CustomItem;
import com.sandcore.items.ItemsManager;
import com.sandcore.levels.LevelManager;

public class ItemUpdateListener implements Listener {
    private final ItemsManager itemsManager;
    private final ClassManager classManager;
    private final LevelManager levelManager;
    private final PlayerDataManager playerDataManager;

    public ItemUpdateListener(SandCore plugin, ItemsManager itemsManager, ClassManager classManager, LevelManager levelManager, PlayerDataManager playerDataManager) {
        this.itemsManager = itemsManager;
        this.classManager = classManager;
        this.levelManager = levelManager;
        this.playerDataManager = playerDataManager;
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
                    
                    // Create final copies for use in lambda
                    final ItemMeta finalMeta = updated.getItemMeta();
                    final ItemStack finalUpdated = updated.clone();
                    final PlayerInteractEvent finalEvent = event;

                    List<String> lore = finalMeta.getLore();
                    if (lore != null) {
                        List<String> newLore = processLoreAsync(lore, customItem, playerClass);
                        
                        // Schedule sync task with final variables
                        Bukkit.getScheduler().runTask(itemsManager.getPlugin().getServer().getPluginManager().getPlugin("SandCore"), () -> {
                            finalMeta.setLore(newLore);
                            finalUpdated.setItemMeta(finalMeta);
                            
                            if (!customItem.getRequiredClasses().contains(playerClass)) {
                                finalEvent.setCancelled(true);
                                player.sendMessage(ChatColor.RED + "This item requires: " + 
                                    String.join(", ", customItem.getRequiredClasses()));
                            }
                            
                            int requiredLevel = customItem.getRequiredLevel();
                            int playerLevel = levelManager.getLevelForXP(
                                playerDataManager.getPlayerData(player.getUniqueId()).getXP()
                            );
                            
                            if (requiredLevel > 0 && playerLevel < requiredLevel) {
                                finalEvent.setCancelled(true);
                                player.sendMessage(ChatColor.RED + "This item requires level " + requiredLevel + 
                                                 " (Your level: " + playerLevel + ")");
                            }
                            
                            player.getInventory().setItem(
                                player.getInventory().getHeldItemSlot(), 
                                finalUpdated
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
            // Create final copy for lambda usage
            final String currentLine = line;
            if (currentLine.contains("{classes}")) {
                String classLine = customItem.getRequiredClasses().stream()
                    .map(cls -> {
                        String formattedName = classManager.getFormattedClassName(cls);
                        ChatColor color = cls.equalsIgnoreCase(playerClass) 
                            ? ChatColor.GREEN 
                            : ChatColor.RED;
                        // Use currentLine instead of line
                        return ChatColor.getLastColors(currentLine.split("\\{classes}")[0]) + color + formattedName;
                    })
                    .collect(Collectors.joining(ChatColor.GRAY + ", "));
                
                line = currentLine.replace("{classes}", classLine);
            }
            if (line.contains("{level}")) {
                line = line.replace("{level}", String.valueOf(customItem.getRequiredLevel()));
            }
            newLore.add(line);
        }
        return newLore;
    }
} 