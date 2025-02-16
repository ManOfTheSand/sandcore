package com.sandcore.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;

import com.sandcore.SandCore;
import com.sandcore.util.ColorParser;

public class CustomItem {
    private String id = "";
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
    private final List<String> requiredClasses = new ArrayList<>();
    private boolean valid = false;
    private int requiredLevel;

    public enum ItemType {
        WEAPON, ARMOR, TOOL, CHARM, OTHER
    }

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    public CustomItem(SandCore plugin, String id, ConfigurationSection config) {
        this.id = id;
        try {
            if(config == null) {
                plugin.getLogger().warning("Null configuration section for item: " + id);
                this.valid = false;
                return;
            }
            
            this.plugin = plugin;
            this.requiredClasses.addAll(config.getStringList("required_classes"));
            this.requiredLevel = config.getInt("level", 0);
            loadFromConfig(config);
            validateItem();
            
            this.valid = true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading item " + id + ": " + e.getMessage());
            this.valid = false;
        }
    }

    private void loadFromConfig(ConfigurationSection config) {
        String materialName = config.getString("item.material", "STONE");
        this.material = Material.matchMaterial(materialName);
        if (this.material == null) {
            this.material = Material.STONE;
        }
        
        this.displayName = ColorParser.parseGradient(config.getString("display-name", ""));
        this.lore = new ArrayList<>();
        for (String line : config.getStringList("lore")) {
            lore.add(ColorParser.parseGradient(line));
        }
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
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
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
        
        // Add this block to handle vanilla attributes
        if (meta != null) {
            // Remove vanilla attributes
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 
                new AttributeModifier(UUID.randomUUID(), "vanilla_remove", 0, 
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, 
                new AttributeModifier(UUID.randomUUID(), "vanilla_remove", 0, 
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
            
            // Hide attributes and unbreakable status
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
        }
        
        // Store version and ID in persistent data
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, id);
        pdc.set(new NamespacedKey(plugin, "item_version"), PersistentDataType.INTEGER, plugin.getItemsManager().getConfigVersion());
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList()));
            
            // Process class placeholder in existing lore
            List<String> processedLore = new ArrayList<>();
            if (this.lore != null) {
                for (String line : this.lore) {
                    if (line.contains("{classes}")) {
                        line = line.replace("{classes}", String.join(", ", requiredClasses));
                    }
                    if (line.contains("{level}")) {
                        line = line.replace("{level}", String.valueOf(requiredLevel));
                    }
                    processedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            
            meta.setLore(processedLore);
            
            // Store requirements in PDC
            pdc.set(new NamespacedKey(plugin, "required_classes"), 
                   PersistentDataType.STRING, 
                   String.join(",", requiredClasses));
            
            // Store level in PDC
            pdc.set(new NamespacedKey(plugin, "required_level"), 
                   PersistentDataType.INTEGER, 
                   requiredLevel);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public boolean matchesVisualIdentity(String displayName, List<String> lore) {
        // Compare parsed display names
        String parsedName = ColorParser.parseGradient(this.displayName);
        if (!parsedName.equals(displayName)) return false;
        
        // Compare parsed lore line-by-line
        if (this.lore.size() != (lore != null ? lore.size() : 0)) return false;
        
        for (int i = 0; i < this.lore.size(); i++) {
            String configuredLine = ColorParser.parseGradient(this.lore.get(i));
            String itemLine = lore.get(i);
            if (!configuredLine.equals(itemLine)) return false;
        }
        
        return true;
    }

    public List<String> getRequiredClasses() {
        return requiredClasses;
    }

    public boolean isValid() {
        return valid;
    }
} 