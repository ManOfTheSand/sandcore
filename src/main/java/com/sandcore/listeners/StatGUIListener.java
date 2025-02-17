package com.sandcore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.stat.StatManager;
import com.sandcore.stat.StatManager.PlayerStats;
import com.sandcore.utils.ItemBuilder;

public class StatGUIListener implements Listener {
    
    private final StatManager statManager;
    private final PlayerDataManager dataManager;
    
    public StatGUIListener(StatManager statManager, PlayerDataManager dataManager) {
        this.statManager = statManager;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Profile")) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null) return;
        
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        PlayerStats stats = statManager.getPlayerStats(player);
        
        // Handle stat point spending
        if (data.getStatPoints() > 0) {
            switch (clicked.getType()) {
                case RED_DYE:
                    stats.increaseAttribute("strength", 1);
                    data.setStatPoints(data.getStatPoints() - 1);
                    break;
                case GREEN_DYE: 
                    stats.increaseAttribute("dexterity", 1);
                    data.setStatPoints(data.getStatPoints() - 1);
                    break;
                case BLUE_DYE:
                    stats.increaseAttribute("intelligence", 1);
                    data.setStatPoints(data.getStatPoints() - 1);
                    break;
            }
            
            // Update GUI and stats
            player.sendMessage("§aStat increased!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            player.openInventory(createUpdatedProfile(player)); // Refresh GUI
        }
    }
    
    private Inventory createUpdatedProfile(Player player) {
        // Get updated player data and stats
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        StatManager.PlayerStats stats = statManager.getPlayerStats(player);
        
        // Create new inventory
        Inventory inv = Bukkit.createInventory(null, 27, "Player Profile");
        
        // Add stats items (same as ProfileGUIListener implementation)
        ItemStack strength = new ItemBuilder(Material.RED_DYE)
            .name("§4Strength: §c" + stats.getAttribute("strength"))
            .lore("§7Allocated: §c" + stats.getAllocatedPoints().getOrDefault("strength", 0))
            .build();
        inv.setItem(10, strength);

        ItemStack dexterity = new ItemBuilder(Material.GREEN_DYE)
            .name("§2Dexterity: §a" + stats.getAttribute("dexterity"))
            .lore("§7Allocated: §a" + stats.getAllocatedPoints().getOrDefault("dexterity", 0)) 
            .build();
        inv.setItem(12, dexterity);

        ItemStack intelligence = new ItemBuilder(Material.BLUE_DYE)
            .name("§9Intelligence: §b" + stats.getAttribute("intelligence"))
            .lore("§7Allocated: §b" + stats.getAllocatedPoints().getOrDefault("intelligence", 0))
            .build();
        inv.setItem(14, intelligence);

        // Stat points display
        ItemStack points = new ItemBuilder(Material.NETHER_STAR)
            .name("§eStat Points: §6" + data.getStatPoints())
            .lore("§7Click attributes to spend points!")
            .build();
        inv.setItem(17, points);

        return inv;
    }
} 