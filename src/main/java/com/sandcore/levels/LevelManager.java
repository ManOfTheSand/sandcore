package com.sandcore.levels;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * LevelManager loads and holds leveling configuration from config.yml.
 *
 * The config.yml must define:
 *  - xpRequirements: a list of XP values for each level (e.g., 0, 100, 300, 600, 1000, ...)
 *  - maxLevel: the maximum level a player can reach.
 */
public class LevelManager {
    private List<Integer> xpRequirements;
    private int maxLevel;
    private final Logger logger;
    
    public LevelManager(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Loads leveling settings from the provided configuration.
     * @param config the FileConfiguration loaded from config.yml.
     */
    public void loadConfiguration(FileConfiguration config) {
        try {
            xpRequirements = config.getIntegerList("xpRequirements");
            if (xpRequirements == null || xpRequirements.isEmpty()) {
                logger.warning("XP requirements not defined in config.yml. Using default values.");
                xpRequirements = List.of(0, 100, 300, 600, 1000);
            }
            maxLevel = config.getInt("maxLevel", xpRequirements.size() - 1);
            if (maxLevel > xpRequirements.size() - 1) {
                logger.warning("maxLevel exceeds the length of xpRequirements list. Adjusting maxLevel.");
                maxLevel = xpRequirements.size() - 1;
            }
            logger.info("Level configuration loaded: maxLevel=" + maxLevel + ", xpRequirements=" + xpRequirements);
        } catch (Exception e) {
            logger.severe("Error loading leveling configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the required XP to reach a given level.
     * @param level the level (0-indexed).
     * @return the XP requirement, or -1 if invalid.
     */
    public int getXPForLevel(int level) {
        if (level < 0 || level >= xpRequirements.size()) {
            return -1;
        }
        return xpRequirements.get(level);
    }
    
    /**
     * Returns the maximum level a player can reach.
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public List<Integer> getXpRequirements() {
        return xpRequirements;
    }
} 