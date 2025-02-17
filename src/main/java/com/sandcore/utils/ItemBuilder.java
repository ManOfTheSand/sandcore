package com.sandcore.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
// ... rest of imports ...

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    // ... class implementation ...

    // Sets the display name
    public ItemBuilder name(String name) {
        meta.setDisplayName(name);
        return this;
    }

    // Sets the lore using an array of strings
    public ItemBuilder lore(String... lore) {
        meta.setLore(Arrays.asList(lore));
        return this;
    }

    // Sets the lore using a list of strings
    public ItemBuilder lore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }

    // Finalizes and returns the customized ItemStack
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
} 