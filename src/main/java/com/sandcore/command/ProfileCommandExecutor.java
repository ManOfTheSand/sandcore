package com.sandcore.command;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sandcore.SandCore;
import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.gui.ProfileGUI;

public class ProfileCommandExecutor implements CommandExecutor {

    private final SandCore plugin;
    private final PlayerDataManager playerDataManager;
    
    public ProfileCommandExecutor(SandCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        Player player = (Player) sender;
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        
        // Load gui.yml from the data folder.
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        // Open the profile GUI and pass the LevelManager.
        ProfileGUI.open(player, data, guiConfig, plugin.getLevelManager());
        return true;
    }
} 