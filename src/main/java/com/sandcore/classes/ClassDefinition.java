package com.sandcore.classes;

public class ClassDefinition {
    private String id;
    private String displayName;
    private String lore;
    private String material;

    public ClassDefinition(String id, String displayName, String lore, String material) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
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
} 