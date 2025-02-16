package com.sandcore.items;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

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

    public enum ItemType {
        WEAPON, ARMOR, TOOL, CHARM, OTHER
    }

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    public CustomItem(String id, ConfigurationSection config) {
        this.id = id;
        loadFromConfig(config);
    }

    private void loadFromConfig(ConfigurationSection config) {
        this.displayName = config.getString("display-name");
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
        Material material = Material.matchMaterial(this.type.name());
        if (material == null) {
            material = Material.STONE;
        }
        return new ItemStack(material);
    }
} 