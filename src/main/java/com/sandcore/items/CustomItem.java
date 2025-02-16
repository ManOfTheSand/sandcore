package com.sandcore.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.sandcore.SandCore;

public class CustomItem {
    private final String id;
    private String displayName;
    private List<String> lore;
    private ItemType type;
    private int level;
    private Rarity rarity;
    private Map<String, Object> attributes;
    private Map<String, Object> effects;
    private String slot;
    private ItemStack baseItem;
    private boolean craftable;
    private List<String> recipe;
    private int recipeGiveAmount;
    private Material material;
    private SandCore plugin;

    public enum ItemType {
        WEAPON, ARMOR, TOOL, CHARM, OTHER
    }

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    public CustomItem(SandCore plugin, String id, ConfigurationSection config) {
        this.plugin = plugin;
        this.id = id;
        loadFromConfig(config);
        validateItem();
    }

    private void loadFromConfig(ConfigurationSection config) {
        String materialName = config.getString("item.material", "STONE");
        this.material = Material.matchMaterial(materialName);
        if (this.material == null) {
            this.material = Material.STONE;
        }
        
        this.displayName = ChatColor.translateAlternateColorCodes('&', 
            config.getString("display-name", "Unnamed Item"));
        this.lore = config.getStringList("lore");
        this.type = ItemType.valueOf(config.getString("type", "OTHER").toUpperCase());
        this.level = config.getInt("level", 1);
        this.rarity = Rarity.valueOf(config.getString("rarity", "COMMON").toUpperCase());
        this.attributes = config.getConfigurationSection("attributes").getValues(false);
        this.effects = config.getConfigurationSection("effects").getValues(false);
        this.slot = config.getString("slot", "mainhand");
        this.craftable = config.getBoolean("craftable", false);
        this.recipe = config.getStringList("recipe");
        this.recipeGiveAmount = config.getInt("recipe-give-amount", 1);
    }

    private void validateItem() {
        if (material == null) {
            plugin.getLogger().warning("Invalid material for item " + id + ", defaulting to STONE");
            material = Material.STONE;
        }
        
        if (displayName == null || displayName.isEmpty()) {
            plugin.getLogger().warning("Missing display name for item " + id);
            displayName = "Unnamed Item";
        }
        
        if (lore == null) {
            lore = new ArrayList<>();
        }
    }

    // Getters and calculation methods
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public ItemType getType() { return type; }
    public int getLevel() { return level; }
    public boolean isCraftable() { return craftable; }
    public List<String> getRecipe() { return recipe; }
    public int getRecipeGiveAmount() { return recipeGiveAmount; }
    public String getSlot() { return slot; }
    public Rarity getRarity() { return rarity; }
    
    public double getAttribute(String key, int playerLevel) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        // TODO: Implement formula parsing with placeholders
        return 0.0;
    }

    public ItemStack buildItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList()));
            
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, id);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
} 