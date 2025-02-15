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
import com.sandcore.levels.LevelManager;
import com.sandcore.classes.ClassManager;

public class ProfileGUI {
    
    public static void open(Player player, PlayerData data, YamlConfiguration guiConfig, LevelManager levelManager, com.sandcore.classes.ClassManager classManager) {
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
        // Compute XP display: show "progress / required" xp
        int currentLevel = data.getLevel();
        int xpForCurrent = levelManager.getXPForLevel(currentLevel);
        int xpForNext = levelManager.getXPForLevel(currentLevel + 1);
        String xpDisplay;
        if (currentLevel >= levelManager.getMaxLevel()) {
            xpDisplay = "MAX";
        } else {
            int progressXP = data.getXP() - xpForCurrent;
            int requiredXP = xpForNext - xpForCurrent;
            xpDisplay = progressXP + " / " + requiredXP;
        }

        for (String line : loreList) {
            // Process each line with our color translator.
            line = com.sandcore.util.ChatUtil.translateColors(line);
            String selectedClassKey = data.getSelectedClass();
            String formattedClass = "None";
            if (!selectedClassKey.isEmpty()) {
                formattedClass = classManager.getFormattedClassName(selectedClassKey);
                if (formattedClass == null || formattedClass.isEmpty()) {
                    formattedClass = selectedClassKey;
                }
            }
            line = line.replace("{selectedClass}", formattedClass);
            line = line.replace("{level}", String.valueOf(data.getLevel()));
            line = line.replace("{xp}", xpDisplay);
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