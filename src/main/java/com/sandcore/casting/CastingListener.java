package com.sandcore.casting;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.ChatColor;

public class CastingListener implements Listener {
    private final CastingManager castingManager;

    public CastingListener(CastingManager castingManager) {
        this.castingManager = castingManager;
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
        castingManager.toggleCastingMode(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            castingManager.handleClick(player, CastingManager.ClickType.LEFT);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            castingManager.handleClick(player, CastingManager.ClickType.RIGHT);
        }

        // Assume that you determine the skill id from the player's action (this is pseudocode).
        // For example, suppose the skill id is determined from the player's click combo:
        String skillId = "fireball"; // Example skill id.
        if (!castingManager.isSkillUnlocked(player, skillId)) {
            player.sendMessage(ChatColor.RED + "You have not unlocked the skill: " + skillId);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onComboClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // Use the already-injected CastingManager instance.
        if (!this.castingManager.isCasting(player)) {
            return;
        }

        // Process valid click actions.
        if (event.getAction() == Action.LEFT_CLICK_AIR ||
            event.getAction() == Action.LEFT_CLICK_BLOCK ||
            event.getAction() == Action.RIGHT_CLICK_AIR ||
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Retrieve the player's class via CastingManager's ClassManager.
            ClassManager.PlayerClass playerClass = this.castingManager.getClassManager().getPlayerClass(player);
            if (playerClass != null) {
                String soundName = playerClass.getCastingSoundName();
                float volume = playerClass.getCastingSoundVolume();
                float pitch = playerClass.getCastingSoundPitch();
                try {
                    Sound sound = Sound.valueOf(soundName);
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("Invalid combo click sound: " + soundName);
                }
            }
        }
    }
} 