package com.sandcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.levels.LevelManager;

public class VanillaXPListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final LevelManager levelManager;

    public VanillaXPListener(PlayerDataManager playerDataManager, LevelManager levelManager) {
        this.playerDataManager = playerDataManager;
        this.levelManager = levelManager;
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // Cancel vanilla XP changes.
        event.setAmount(0);

        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        int currentLevel = data.getLevel();
        int currentXP = data.getXP();
        int xpForCurrent = levelManager.getXPForLevel(currentLevel);
        int xpForNext = levelManager.getXPForLevel(currentLevel + 1);
        float progress = 0;
        if ((xpForNext - xpForCurrent) > 0) {
            progress = (float) (currentXP - xpForCurrent) / (xpForNext - xpForCurrent);
        }
        // Update the XP bar to reflect your internal XP system.
        player.setExp(progress);
        player.setLevel(currentLevel);
    }
} 