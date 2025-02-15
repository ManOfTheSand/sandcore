package com.sandcore.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sandcore.SandCore;
import com.sandcore.classes.ClassDefinition;
import com.sandcore.classes.ClassManager;

/**
 * ClassInfoCommandExecutor handles the /classinfo command.
 * - With no arguments, it displays the sender's current class.
 * - With a player's name as an argument, it displays that player's class (requires appropriate permission).
 */
public class ClassInfoCommandExecutor implements CommandExecutor {

    private SandCore plugin;
    private ClassManager classManager;
    
    public ClassInfoCommandExecutor(SandCore plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("Executing /classinfo command, args length: " + args.length);
        
        // When no arguments are provided, show the sender's own class.
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot check its own class.");
                return true;
            }
            Player player = (Player) sender;
            ClassDefinition def = classManager.getPlayerClass(player);
            if (def == null) {
                player.sendMessage(ChatColor.YELLOW + "You do not have a class set.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Your class is: " 
                        + ChatColor.translateAlternateColorCodes('&', def.getDisplayName()));
            }
            return true;
        } else {
            // For players with permission, check another specified player's class.
            if (!sender.hasPermission("sandmmo.admin.classinfo")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to check others' classes.");
                return true;
            }
            String targetName = args[0];
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player " + targetName + " is not online.");
                return true;
            }
            ClassDefinition def = classManager.getPlayerClass(target);
            if (def == null) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " does not have a class set.");
            } else {
                sender.sendMessage(ChatColor.GREEN + target.getName() + "'s class is: " 
                        + ChatColor.translateAlternateColorCodes('&', def.getDisplayName()));
            }
            return true;
        }
    }
} 