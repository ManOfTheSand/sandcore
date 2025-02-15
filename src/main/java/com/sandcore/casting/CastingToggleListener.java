package com.sandcore.casting;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class CastingToggleListener implements Listener {

    private final CastingManager castingManager;
    
    public CastingToggleListener(CastingManager castingManager) {
        this.castingManager = castingManager;
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Use the off-hand (F key) to toggle casting mode.
        event.setCancelled(true);
        Player player = event.getPlayer();
        castingManager.toggleCastingMode(player);
    }
} 