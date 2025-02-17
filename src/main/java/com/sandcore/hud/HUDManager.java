package com.sandcore.hud;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.sandcore.SandCore;
import com.sandcore.data.PlayerData;

/**
 * HUDManager updates the player's HUD to show the plugin's leveling system.
 * It uses a scoreboard objective to display the custom level and disables the vanilla XP display.
 */
public class HUDManager {
    private final SandCore plugin;
    private final Logger logger;
    
    public HUDManager(SandCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Updates the player's HUD to reflect the plugin-managed level.
     * @param player the player whose HUD will be updated.
     * @param data the player's persistent leveling data.
     */
    public void updateHUD(Player player, PlayerData data) {
        try {
            Scoreboard board = player.getScoreboard();
            if (board == null) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
            }
            Objective objective = board.getObjective("customLevel");
            if (objective == null) {
                objective = board.registerNewObjective("customLevel", "dummy", "Level");
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            objective.getScore(player.getName()).setScore(data.getLevel());
            player.setScoreboard(board);
            
            // Disable the vanilla XP display.
            player.setExp(0);
            player.setLevel(data.getLevel());
            
            logger.info("Updated HUD for " + player.getName() + " to level " + data.getLevel());
        } catch (Exception e) {
            logger.severe("Error updating HUD for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadHUD() {
        logger.info("Reloaded HUD configuration");
    }
} 