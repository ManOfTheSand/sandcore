package com.sandcore.items;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.sandcore.SandCore;

public class ItemsManager {
    private final SandCore plugin;
    private final Map<String, CustomItem> itemMap = new HashMap<>();
    private File itemsFile;

    public ItemsManager(SandCore plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        itemMap.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "items.yml"));
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        
        if(itemsSection != null) {
            for(String itemId : itemsSection.getKeys(false)) {
                // Store original case in map but allow lowercase lookup
                itemMap.put(itemId.toLowerCase(), new CustomItem(plugin, itemId, itemsSection.getConfigurationSection(itemId)));
                plugin.getLogger().info("Loaded item: " + itemId);
            }
        }
    }

    private void registerCrafting(CustomItem item) {
        if (item.isCraftable()) {
            // TODO: Implement recipe registration
        }
    }

    public CustomItem getItem(String id) {
        // Case-insensitive lookup
        return itemMap.get(id.toLowerCase());
    }

    public void reloadItems() {
        try {
            loadItems();
            plugin.getLogger().info("Successfully reloaded " + itemMap.size() + " items");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CustomItem getItemFromStack(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String itemId = pdc.get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
        
        return itemId != null ? itemMap.get(itemId.toLowerCase()) : null;
    }

    public List<String> getItemIds() {
        return new ArrayList<>(itemMap.keySet());
    }

    public int getItemCount() {
        return itemMap.size();
    }
} 