package com.sandcore.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sandcore.SandCore;
import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.hud.HUDManager;
import com.sandcore.levels.LevelManager;

/**
 * DebugXPCommandExecutor provides detailed debug information about a player's XP and level.
 * 
 * Usage:
 *   /debugxp [player] [xpAmount]
 *     - If only player is supplied (or if run by a player with no arguments), this command displays
 *       the current XP, level, next level requirement, XP requirements array, and maximum level.
 *     - If a second argument xpAmount is provided, it simulates awarding that XP to the target player
 *       (updating their XP/level accordingly), then displays updated debug information.
 * 
 * Permission required: sandcore.admin.debugxp
 */
public class DebugXPCommandExecutor implements CommandExecutor {

    private final SandCore plugin;
    private final LevelManager levelManager;
    private final PlayerDataManager playerDataManager;
    private final HUDManager hudManager;
    
    public DebugXPCommandExecutor(SandCore plugin, LevelManager levelManager, 
                                  PlayerDataManager playerDataManager, HUDManager hudManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.playerDataManager = playerDataManager;
        this.hudManager = hudManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Usage: /debugxp [player] [xpAmount]
        // Determine target player: use provided argument or sender (if it's a player)
        Player target = null;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer " + args[0] + " is not online!");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cYou must specify a player when running this command from console.");
            return true;
        }
        
        // Retrieve persistent data for the player.
        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        
        // If an XP amount is provided, simulate awarding XP.
        if (args.length >= 2) {
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cInvalid XP amount provided.");
                return true;
            }
            int previousLevel = data.getLevel();
            int previousXP = data.getXP();
            boolean leveledUp = data.addXP(amount, levelManager);
            hudManager.updateHUD(target, data);
            sender.sendMessage("§aAwarded " + amount + " XP to " + target.getName() + ".");
            target.sendMessage("§aYou have received an extra " + amount + " XP (Debug command).");
            plugin.getLogger().info("Debug: " + sender.getName() + " awarded " + amount + " XP to " +
                    target.getName() + " (Level " + previousLevel + " [" + previousXP + " XP] -> " +
                    data.getLevel() + " [" + data.getXP() + " XP]).");
            if (leveledUp) {
                target.sendMessage("§aYou leveled up!");
            }
        }
        
        // Display debug information to the sender.
        int currentLevel = data.getLevel();
        int currentXP = data.getXP();
        int maxLevel = levelManager.getMaxLevel();
        int nextLevelXP = (currentLevel < maxLevel) ? levelManager.getXPForLevel(currentLevel + 1) : -1;
        
        sender.sendMessage("§e----- Debug XP Info for " + target.getName() + " -----");
        sender.sendMessage("§eLevel: " + currentLevel);
        sender.sendMessage("§eXP: " + currentXP);
        if (nextLevelXP != -1) {
            sender.sendMessage("§eXP required for next level: " + nextLevelXP);
        } else {
            sender.sendMessage("§ePlayer is at max level.");
        }
        sender.sendMessage("§eXP Requirements List: " + levelManager.getXpRequirements().toString());
        sender.sendMessage("§eMax Level: " + maxLevel);
        
        return true;
    }
} 