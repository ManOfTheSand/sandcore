package com.sandcore.casting;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CastingClickListener implements Listener {

    private final CastingManager castingManager;
    
    public CastingClickListener(CastingManager castingManager) {
        this.castingManager = castingManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!castingManager.isInCastingMode(player)) {
            return;
        }
        
        Action action = event.getAction();
        String clickType = null;
        switch (action) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                clickType = "L";
                break;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                clickType = "R";
                break;
            default:
                return;
        }
        
        event.setCancelled(true);
        castingManager.registerClick(player, clickType);
    }
} 