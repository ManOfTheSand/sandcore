package com.sandcore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.levels.LevelManager;
import com.sandcore.stat.StatManager;
import com.sandcore.stat.StatManager.PlayerStats;

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
        // Recreate the profile GUI with updated values
        // ... same as ProfileGUIListener implementation ...
    }
} 