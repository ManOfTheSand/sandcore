package com.sandcore.items;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.sandcore.SandCore;

public class ItemsManager {
    private final SandCore plugin;
    private final Map<String, CustomItem> itemMap = new HashMap<>();
    private File itemsFile;
    private int configVersion = 0;

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
            configVersion++;
            loadItems();
            plugin.getLogger().info("Successfully reloaded " + itemMap.size() + " items (v" + configVersion + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getConfigVersion() {
        return configVersion;
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

    public void updateAllItemsInWorld() {
        // Update player inventories
        for(Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerItems(player);
        }
        
        // TODO: Add logic to update items in chests/other containers
    }

    private void updatePlayerItems(Player player) {
        updateInventoryItems(player.getInventory());
        updateInventoryItems(player.getEnderChest());
    }

    private void updateInventoryItems(Inventory inventory) {
        for(int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if(item != null) {
                ItemStack updated = getUpdatedItem(item);
                if(updated != null) {
                    inventory.setItem(i, updated);
                }
            }
        }
    }

    public ItemStack getUpdatedItem(ItemStack oldItem) {
        CustomItem currentVersion = getItemFromStack(oldItem);
        if(currentVersion == null) return null;
        
        int storedVersion = getItemVersion(oldItem);
        if(storedVersion < configVersion) {
            ItemStack newItem = currentVersion.buildItem();
            newItem.setAmount(oldItem.getAmount());
            return newItem;
        }
        return null;
    }

    private int getItemVersion(ItemStack item) {
        if(!item.hasItemMeta()) return -1;
        return item.getItemMeta().getPersistentDataContainer()
            .getOrDefault(new NamespacedKey(plugin, "item_version"), PersistentDataType.INTEGER, -1);
    }
} 