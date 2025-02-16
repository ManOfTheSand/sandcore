package com.sandcore.items;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import com.sandcore.SandCore;

public class ItemsManager {
    private final SandCore plugin;
    private final Map<String, CustomItem> items = new HashMap<>();
    private File itemsFile;

    public ItemsManager(SandCore plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        // First check/create file synchronously
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
            plugin.getLogger().info("Created default items.yml");
        }

        // Then load content async
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
                    items.clear();
                    
                    for (String itemId : config.getConfigurationSection("items").getKeys(false)) {
                        CustomItem item = new CustomItem(plugin, itemId, 
                            config.getConfigurationSection("items." + itemId));
                        items.put(itemId, item);
                        registerCrafting(item);
                    }
                    plugin.getLogger().log(Level.INFO, "Loaded " + items.size() + " custom items");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error loading items.yml", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void registerCrafting(CustomItem item) {
        if (item.isCraftable()) {
            // TODO: Implement recipe registration
        }
    }

    public CustomItem getItem(String id) {
        return items.get(id.toLowerCase());
    }

    public void reloadItems() {
        loadItems();
    }

    public CustomItem getItemFromStack(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String itemId = pdc.get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
        
        return itemId != null ? items.get(itemId) : null;
    }

    public List<String> getItemIds() {
        return new ArrayList<>(items.keySet());
    }
} 