package com.sandcore.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.classes.ClassManager;
import com.sandcore.classes.ClassSelectionGUI;

/**
 * ClassesCommandExecutor handles the /classes command.
 * When executed by a player, it opens the class selection GUI.
 */
public class ClassesCommandExecutor implements CommandExecutor {

    private JavaPlugin plugin;
    private ClassManager classManager;

    public ClassesCommandExecutor(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }
        Player player = (Player) sender;
        try {
            // Open the class selection GUI.
            new ClassSelectionGUI(plugin, classManager).openGUI(player);
            plugin.getLogger().info("Player " + player.getName() + " opened the class selection GUI.");
        } catch (Exception e) {
            sender.sendMessage("An error occurred while opening the class selection GUI.");
            plugin.getLogger().severe("Error in /classes command: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
} 