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
import org.bukkit.inventory.meta.ItemMeta;
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
        
        // Ensure data folder exists
        plugin.getDataFolder().mkdirs();
        
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
            plugin.getLogger().info("Created default items.yml");
            // Reload after creating
            itemsFile = new File(plugin.getDataFolder(), "items.yml"); 
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        
        if(itemsSection != null) {
            for(String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                if(itemSection != null) { // Add null check
                    CustomItem item = new CustomItem(plugin, itemId, itemSection);
                    if(item.isValid()) { // Add validation check
                        itemMap.put(itemId.toLowerCase(), item);
                        plugin.getLogger().info("Loaded item: " + itemId);
                    }
                }
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
        
        // Add null check and fallback to display name matching
        if (itemId == null) {
            return findItemByDisplayProperties(stack);
        }
        
        return itemMap.get(itemId.toLowerCase());
    }

    private CustomItem findItemByDisplayProperties(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        String displayName = meta.getDisplayName();
        List<String> lore = meta.getLore();

        // Match against all registered items
        for (CustomItem item : itemMap.values()) {
            if (item.matchesVisualIdentity(displayName, lore)) {
                // Update legacy items with proper NBT tags
                ItemStack updated = item.buildItem();
                updated.setAmount(stack.getAmount());
                return item;
            }
        }
        return null;
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
        if (currentVersion == null) return null;
        
        // Handle legacy items without version tags
        int storedVersion = getItemVersion(oldItem);
        if (storedVersion == -1) {
            plugin.getLogger().info("Updating legacy item: " + currentVersion.getId());
            ItemStack newItem = currentVersion.buildItem();
            newItem.setAmount(oldItem.getAmount());
            return newItem;
        }
        
        if (storedVersion < configVersion) {
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

    public boolean isSandCoreItem(ItemStack stack) {
        return getItemFromStack(stack) != null;
    }

    public SandCore getPlugin() {
        return plugin;
    }
} 