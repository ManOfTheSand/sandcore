package com.sandcore.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages persistent storage of player leveling data.
 * Data is persisted in a file (playerdata.yml) so that XP and level persist across class changes.
 */
public class PlayerDataManager {
    private final Map<UUID, PlayerData> playerDataMap;
    private final File playerDataFile;
    private YamlConfiguration playerDataConfig;
    private final Logger logger;
    
    public PlayerDataManager(File dataFolder, Logger logger) {
        this.logger = logger;
        playerDataMap = new HashMap<>();
        playerDataFile = new File(dataFolder, "playerdata.yml");
        loadPlayerData();
    }
    
    /**
     * Loads player data from playerdata.yml.
     */
    public void loadPlayerData() {
        try {
            if (!playerDataFile.exists()) {
                playerDataFile.createNewFile();
            }
            playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
            if (playerDataConfig.contains("players")) {
                for (String key : playerDataConfig.getConfigurationSection("players").getKeys(false)) {
                    UUID uuid = UUID.fromString(key);
                    int xp = playerDataConfig.getInt("players." + key + ".xp", 0);
                    int level = playerDataConfig.getInt("players." + key + ".level", 0);
                    PlayerData data = new PlayerData(uuid);
                    data.setXP(xp);
                    data.setLevel(level);
                    playerDataMap.put(uuid, data);
                }
            }
            logger.info("Loaded player data: " + playerDataMap.size() + " entries.");
        } catch (Exception e) {
            logger.severe("Error loading player data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Saves current player data to playerdata.yml.
     */
    public void savePlayerData() {
        try {
            for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerData data = entry.getValue();
                playerDataConfig.set("players." + uuid.toString() + ".xp", data.getXP());
                playerDataConfig.set("players." + uuid.toString() + ".level", data.getLevel());
            }
            playerDataConfig.save(playerDataFile);
            logger.info("Player data saved successfully.");
        } catch (Exception e) {
            logger.severe("Error saving player data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Returns the PlayerData for the given player's UUID. If none exists, a new PlayerData is created.
     * @param uuid the player's UUID.
     * @return the PlayerData instance.
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (!playerDataMap.containsKey(uuid)) {
            PlayerData data = new PlayerData(uuid);
            playerDataMap.put(uuid, data);
        }
        return playerDataMap.get(uuid);
    }
} 