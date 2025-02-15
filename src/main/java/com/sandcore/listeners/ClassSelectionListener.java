package com.sandcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

import com.sandcore.SandCore;
import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;

public class ClassSelectionListener implements Listener {

    private final SandCore plugin;
    private final PlayerDataManager playerDataManager;

    public ClassSelectionListener(SandCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        // Check if this inventory is the Class Selection GUI by title or a config flag.
        if (ChatColor.stripColor(view.getTitle()).equalsIgnoreCase("Class Selection")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            // Assume the display name of the clicked item is the class name.
            String className = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            // Update the player's data with the selected class.
            PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
            data.setSelectedClass(className);
            // Save the player data so that it persists.
            playerDataManager.savePlayerData();
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "You have selected the " + className + " class.");
        }
    }
} 