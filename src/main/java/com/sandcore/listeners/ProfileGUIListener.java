package com.sandcore.listeners;

import java.util.List;
import java.util.stream.Collectors;

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

import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.stat.StatManager;
import com.sandcore.stat.StatManager.PlayerStats;
import com.sandcore.util.ChatUtil;
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
            guiConfig.getString("profileGUI.title", "Player Profile"));
        int size = guiConfig.getInt("profileGUI.size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        StatManager.PlayerStats stats = statManager.getPlayerStats(player);
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        
        // Load ALL items from config
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("profileGUI.items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                loadGuiItem(inv, itemKey, stats, data);
            }
        }
        
        return inv;
    }

    private void loadGuiItem(Inventory inv, String attribute, PlayerStats stats, PlayerData data) {
        ConfigurationSection section = guiConfig.getConfigurationSection("profileGUI.items." + attribute);
        if (section == null) {
            Bukkit.getLogger().warning("[SandCore] Missing GUI config section for: " + attribute);
            return;
        }
        
        Material material = Material.matchMaterial(section.getString("material"));
        if (material == null) {
            Bukkit.getLogger().warning("[SandCore] Invalid material for: " + attribute);
            return;
        }

        ItemStack item = new ItemBuilder(material)
            .name(replacePlaceholders(section.getString("name"), stats, attribute))
            .lore(replacePlaceholders(section.getStringList("lore"), stats, attribute))
            .build();
        
        int slot = section.getInt("slot", -1);
        if (slot == -1) {
            Bukkit.getLogger().warning("[SandCore] Missing slot for: " + attribute);
            return;
        }
        
        inv.setItem(slot, item);
        Bukkit.getLogger().info("[SandCore] Loaded GUI item: " + attribute + " at slot " + slot);
    }

    private String replacePlaceholders(String text, PlayerStats stats, String attribute) {
        String output = text.replace("{value}", String.format("%.1f", stats.getAttribute(attribute)))
                           .replace("{allocated}", String.valueOf(stats.getAllocatedPoints().getOrDefault(attribute, 0)));
        return ChatUtil.translateGradientsAndHex(output);
    }

    private List<String> replacePlaceholders(List<String> lore, PlayerStats stats, String attribute) {
        return lore.stream()
            .map(line -> ChatUtil.translateGradientsAndHex(
                line.replace("{value}", String.format("%.1f", stats.getAttribute(attribute)))
                   .replace("{allocated}", String.valueOf(stats.getAllocatedPoints().getOrDefault(attribute, 0)))
            ))
            .collect(Collectors.toList());
    }

    private String replacePlaceholders(String text, PlayerData data) {
        return ChatUtil.translateGradientsAndHex(
            text.replace("{points}", String.valueOf(data.getStatPoints()))
        );
    }

    private List<String> replacePlaceholders(List<String> lore, PlayerData data) {
        return lore.stream()
            .map(line -> ChatUtil.translateGradientsAndHex(
                line.replace("{points}", String.valueOf(data.getStatPoints()))
            ))
            .collect(Collectors.toList());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String expectedTitle = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("profileGUI.title", "Profile"));
        
        if (event.getView().getTitle().equals(expectedTitle)) {
            event.setCancelled(true); // Cancel all clicks in this GUI
            
            // Handle stat point allocation here if needed
        }
    }
} 