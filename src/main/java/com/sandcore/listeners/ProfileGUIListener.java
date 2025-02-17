package com.sandcore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.stat.StatManager;
import com.sandcore.stat.StatManager.PlayerStats;
import com.sandcore.utils.ItemBuilder;

public class ProfileGUIListener implements Listener {

    private final StatManager statManager;
    private final FileConfiguration guiConfig;
    private PlayerDataManager playerDataManager;

    public ProfileGUIListener(StatManager statManager, FileConfiguration guiConfig) {
        this.statManager = statManager;
        this.guiConfig = guiConfig;
    }

    public void setPlayerDataManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    public Inventory createProfileGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("profile.title", "Player Profile"));
        int size = guiConfig.getInt("profile.size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        StatManager.PlayerStats stats = statManager.getPlayerStats(player);
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        
        // Load attribute items
        loadGuiItem(inv, "strength", stats, data);
        loadGuiItem(inv, "dexterity", stats, data);
        loadGuiItem(inv, "intelligence", stats, data);
        
        // Load stat points
        ConfigurationSection pointsSection = guiConfig.getConfigurationSection("profile.items.stat_points");
        if (pointsSection != null) {
            ItemStack pointsItem = new ItemBuilder(Material.matchMaterial(pointsSection.getString("material")))
                .name(replacePlaceholders(pointsSection.getString("name"), data))
                .lore(replacePlaceholders(pointsSection.getStringList("lore"), data))
                .build();
            inv.setItem(pointsSection.getInt("slot"), pointsItem);
        }
        
        return inv;
    }

    private void loadGuiItem(Inventory inv, String attribute, PlayerStats stats, PlayerData data) {
        ConfigurationSection section = guiConfig.getConfigurationSection("profile.items." + attribute);
        if (section == null) return;

        ItemStack item = new ItemBuilder(Material.matchMaterial(section.getString("material")))
            .name(replacePlaceholders(section.getString("name"), stats, attribute))
            .lore(replacePlaceholders(section.getStringList("lore"), stats, attribute))
            .build();
        
        inv.setItem(section.getInt("slot"), item);
    }

    private String replacePlaceholders(String text, PlayerStats stats, String attribute) {
        return text.replace("{value}", String.format("%.1f", stats.getAttribute(attribute)))
                  .replace("{allocated}", String.valueOf(stats.getAllocatedPoints().getOrDefault(attribute, 0)));
    }

    private List<String> replacePlaceholders(List<String> lore, PlayerStats stats, String attribute) {
        return lore.stream()
            .map(line -> line.replace("{value}", String.format("%.1f", stats.getAttribute(attribute)))
                            .replace("{allocated}", String.valueOf(stats.getAllocatedPoints().getOrDefault(attribute, 0))))
            .collect(Collectors.toList());
    }

    private String replacePlaceholders(String text, PlayerData data) {
        return text.replace("{points}", String.valueOf(data.getStatPoints()));
    }

    private List<String> replacePlaceholders(List<String> lore, PlayerData data) {
        return lore.stream()
            .map(line -> line.replace("{points}", String.valueOf(data.getStatPoints())))
            .collect(Collectors.toList());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if(event.getInventory().getHolder() instanceof com.sandcore.gui.ProfileGUIHolder) {
            event.setCancelled(true);
        }
    }
} 