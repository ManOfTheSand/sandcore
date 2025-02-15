package com.sandcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.sandcore.data.PlayerData;

public class ProfileGUI {
    
    public static void open(Player player, PlayerData data, YamlConfiguration guiConfig) {
        // Retrieve profile GUI parameters from gui.yml
        String section = "profileGUI";
        String rawTitle = guiConfig.getString(section + ".title", "&6Profile");
        String title = com.sandcore.util.ChatUtil.translateColors(rawTitle);
        int size = guiConfig.getInt(section + ".size", 9);
        Inventory inventory = Bukkit.createInventory(new ProfileGUIHolder(), size, title);
        
        // Retrieve the configuration for the profile item.
        String itemPath = section + ".items.profileItem";
        int slot = guiConfig.getInt(itemPath + ".slot", 4);
        String materialName = guiConfig.getString(itemPath + ".material", "PAPER");
        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String displayName = com.sandcore.util.ChatUtil.translateColors(
                guiConfig.getString(itemPath + ".displayName", "&aProfile Info"));
        meta.setDisplayName(displayName);
        
        // Process lore text with placeholders for player's selected class, level, and XP.
        List<String> loreList = guiConfig.getStringList(itemPath + ".lore");
        List<String> processedLore = new ArrayList<>();
        for (String line : loreList) {
            // Process each line with our color translator.
            line = com.sandcore.util.ChatUtil.translateColors(line);
            String selected = data.getSelectedClass().isEmpty() ? "None" : data.getSelectedClass();
            line = line.replace("{selectedClass}", selected);
            line = line.replace("{level}", String.valueOf(data.getLevel()));
            line = line.replace("{xp}", String.valueOf(data.getXP()));
            processedLore.add(line);
        }
        meta.setLore(processedLore);
        item.setItemMeta(meta);
        
        // Place the item in the specified slot.
        inventory.setItem(slot, item);
        
        // Lock the inventory so that items cannot be removed.
        // (You should have an InventoryClickListener that cancels clicks in this inventory.)
        
        // Open the inventory for the player.
        player.openInventory(inventory);
    }
} 