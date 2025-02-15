package com.sandcore.data;

import java.util.UUID;

/**
 * Represents persistent leveling data for a player.
 * This data includes the player's XP and level.
 */
public class PlayerData {
    private final UUID playerUUID;
    private int xp;
    private int level;
    
    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.xp = 0;
        this.level = 0;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public int getXP() {
        return xp;
    }
    
    public void setXP(int xp) {
        this.xp = xp;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    /**
     * Adds XP to the player's current total. If the total XP exceeds the threshold
     * for the next level (as defined by the LevelManager), the player levels up.
     *
     * @param amount the XP to add.
     * @param levelManager the LevelManager instance used to determine XP thresholds.
     * @return true if the player leveled up; false otherwise.
     */
    public boolean addXP(int amount, com.sandcore.levels.LevelManager levelManager) {
        if (amount < 0) return false;
        
        // If the player is already at max level, cap XP to maximum and return.
        if (level >= levelManager.getMaxLevel()) {
            xp = levelManager.getXPForLevel(levelManager.getMaxLevel());
            return false;
        }
        
        xp += amount;
        boolean leveledUp = false;
        
        while (level < levelManager.getMaxLevel()) {
            int requiredXP = levelManager.getXPForLevel(level + 1);
            if (xp >= requiredXP) {
                level++;
                leveledUp = true;
            } else {
                break;
            }
        }
        
        // Once maximum level is reached, cap XP at the required amount for max level.
        if (level == levelManager.getMaxLevel()) {
            xp = levelManager.getXPForLevel(level);
        }
        return leveledUp;
    }
} 