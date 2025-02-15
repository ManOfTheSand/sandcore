package com.sandcore.command;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sandcore.SandCore;
import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.hud.HUDManager;
import com.sandcore.levels.LevelManager;

public class SetLevelCommandExecutor implements CommandExecutor {

    private final SandCore plugin;
    private final LevelManager levelManager;
    private final PlayerDataManager playerDataManager;
    private final HUDManager hudManager;
    
    public SetLevelCommandExecutor(SandCore plugin, LevelManager levelManager, 
                                   PlayerDataManager playerDataManager, HUDManager hudManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.playerDataManager = playerDataManager;
        this.hudManager = hudManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Usage: /setlevel <player> <level>
        if (!sender.hasPermission("sandcore.admin.setlevel")) {
            sender.sendMessage("§cYou do not have permission to run this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /setlevel <player> <level>");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer " + args[0] + " is not online!");
            return true;
        }
        
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid level. Must be a number.");
            return true;
        }
        
        // Validate level range.
        if (level < 0 || level > levelManager.getMaxLevel()) {
            sender.sendMessage("§cInvalid level. Must be between 0 and " + levelManager.getMaxLevel());
            return true;
        }
        
        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        data.setLevel(level);
        // Set XP to the minimum required for that level.
        int xpForLevel = levelManager.getXPForLevel(level);
        data.setXP(xpForLevel);
        
        hudManager.updateHUD(target, data);
        plugin.getLogger().info("Admin " + sender.getName() + " set " + target.getName() + "'s level to " + level);
        sender.sendMessage("§aSet " + target.getName() + "'s level to " + level);
        target.sendMessage("§aYour level has been set to " + level);
        
        // Provide level up feedback.
        String msgTemplate = plugin.getConfig().getString("levelUp.message", 
                "§aCongratulations, you have reached level {level}!");
        String levelUpMessage = msgTemplate.replace("{level}", String.valueOf(level));
        target.sendMessage(levelUpMessage);
        
        String soundStr = plugin.getConfig().getString("levelUp.sound", "ENTITY_PLAYER_LEVELUP");
        try {
            Sound sound = Sound.valueOf(soundStr.toUpperCase());
            target.playSound(target.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid level up sound in config: " + soundStr);
        }
        return true;
    }
} 