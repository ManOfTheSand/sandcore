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
import com.sandcore.util.ChatUtil;

public class GiveXPCommandExecutor implements CommandExecutor {
    
    private final SandCore plugin;
    private final LevelManager levelManager;
    private final PlayerDataManager playerDataManager;
    private final HUDManager hudManager;
    
    public GiveXPCommandExecutor(SandCore plugin, LevelManager levelManager, 
                                 PlayerDataManager playerDataManager, HUDManager hudManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.playerDataManager = playerDataManager;
        this.hudManager = hudManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Usage: /givexp <player> <amount>
        if (!sender.hasPermission("sandcore.admin.givexp")) {
            sender.sendMessage("§cYou do not have permission to run this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /givexp <player> <amount>");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer " + args[0] + " is not online!");
            return true;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid XP amount. Must be a number.");
            return true;
        }
        
        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        int oldLevel = data.getLevel();
        boolean leveledUp = data.addXP(amount, levelManager);
        
        plugin.getLogger().info("Admin " + sender.getName() + " gave " + amount + " XP to " 
                + target.getName() + ". New XP: " + data.getXP() + ", Level: " + data.getLevel());
        hudManager.updateHUD(target, data);
        
        sender.sendMessage("§aGave " + amount + " XP to " + target.getName() + ".");
        target.sendMessage("§aYou have been awarded " + amount + " XP.");
        
        // If the player leveled up, send a custom message and play a sound from the config.
        if (leveledUp && data.getLevel() > oldLevel) {
            String msgTemplate = plugin.getConfig().getString("levelUp.message", 
                    "§aCongratulations, you have reached level {level}!");
            String levelUpMessage = ChatUtil.translateHexColorCodes(
                    msgTemplate.replace("{level}", String.valueOf(data.getLevel()))
            );
            target.sendMessage(levelUpMessage);
            
            String soundStr = plugin.getConfig().getString("levelUp.sound", "ENTITY_PLAYER_LEVELUP");
            try {
                Sound sound = Sound.valueOf(soundStr.toUpperCase());
                target.playSound(target.getLocation(), sound, 1.0F, 1.0F);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid level up sound in config: " + soundStr);
            }
        }
        return true;
    }
} 