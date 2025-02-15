package com.sandcore.levels;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * XPSourceManager loads XP gain values for mob kills from xp-sources.yml.
 *
 * The xp-sources.yml file should have sections for both vanilla and mythic mobs:
 * 
 * vanilla:
 *   ZOMBIE: 50
 *   SKELETON: 60
 *   CREEPER: 70
 * mythic:
 *   MYTHIC_ZOMBIE: 100
 *   MYTHIC_SKELETON: 120
 */
public class XPSourceManager {
    private Map<String, Integer> vanillaXP;
    private Map<String, Integer> mythicXP;
    private final Logger logger;
    
    public XPSourceManager(Logger logger) {
        this.logger = logger;
        vanillaXP = new HashMap<>();
        mythicXP = new HashMap<>();
    }
    
    /**
     * Loads XP sources from the specified xp-sources.yml file.
     * @param file the xp-sources.yml file.
     */
    public void loadXPSources(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection vanillaSection = config.getConfigurationSection("vanilla");
            if (vanillaSection != null) {
                for (String key : vanillaSection.getKeys(false)) {
                    int xp = vanillaSection.getInt(key, 0);
                    vanillaXP.put(key.toUpperCase(), xp);
                }
            } else {
                logger.warning("No 'vanilla' section found in xp-sources.yml.");
            }
            
            ConfigurationSection mythicSection = config.getConfigurationSection("mythic");
            if (mythicSection != null) {
                for (String key : mythicSection.getKeys(false)) {
                    int xp = mythicSection.getInt(key, 0);
                    mythicXP.put(key.toUpperCase(), xp);
                }
            } else {
                logger.warning("No 'mythic' section found in xp-sources.yml.");
            }
            
            logger.info("XP sources loaded: vanilla=" + vanillaXP + ", mythic=" + mythicXP);
        } catch (Exception e) {
            logger.severe("Error loading xp-sources.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Returns the XP awarded for killing a mob of the given type.
     * @param mobType the mob type (e.g., "ZOMBIE").
     * @return the XP value, or 0 if not defined.
     */
    public int getXPForMob(String mobType) {
        if (mobType == null) return 0;
        String type = mobType.toUpperCase();
        if (vanillaXP.containsKey(type)) {
            return vanillaXP.get(type);
        } else if (mythicXP.containsKey(type)) {
            return mythicXP.get(type);
        }
        return 0;
    }
} 