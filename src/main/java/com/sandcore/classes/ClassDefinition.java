package com.sandcore.classes;

import java.util.Map;

public class ClassDefinition {
    private String id;
    private String displayName;
    private String lore;
    private String material;
    private int slot;
    private Map<String, String> keyCombos;

    public ClassDefinition(String id, String displayName, String lore, String material, int slot) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.slot = slot;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLore() {
        return lore;
    }

    public String getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public Map<String, String> getKeyCombos() {
        return keyCombos;
    }

    public void setKeyCombos(Map<String, String> keyCombos) {
        this.keyCombos = keyCombos;
    }
} 