package com.sandcore.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.sandcore.SandCore;
import com.sandcore.listeners.ProfileGUIListener;

public class ProfileCommandExecutor implements CommandExecutor {

    private final SandCore plugin;
    private final ProfileGUIListener profileGUIListener;
    
    public ProfileCommandExecutor(SandCore plugin, ProfileGUIListener profileGUIListener) {
        this.plugin = plugin;
        this.profileGUIListener = profileGUIListener;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        Player player = (Player) sender;
        Inventory profileGUI = profileGUIListener.createProfileGUI(player);
        player.openInventory(profileGUI);
        return true;
    }
} 