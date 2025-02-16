package com.sandcore.items;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ItemsManager {
    private final JavaPlugin plugin;
    private final Map<String, CustomItem> items = new HashMap<>();
    private File itemsFile;

    public ItemsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    itemsFile = new File(plugin.getDataFolder(), "items.yml");
                    if (!itemsFile.exists()) {
                        plugin.saveResource("items.yml", false);
                    }
                    
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
                    items.clear();
                    
                    for (String itemId : config.getConfigurationSection("items").getKeys(false)) {
                        CustomItem item = new CustomItem(itemId, 
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
} 