package com.sandcore.casting;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CastingClickListener implements Listener {

    private final CastingManager castingManager;
    
    public CastingClickListener(CastingManager castingManager) {
        this.castingManager = castingManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only process main-hand events to avoid duplicate counting from off-hand clicks.
        if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND)) {
            return;
        }
        
        Player player = event.getPlayer();
        Action action = event.getAction();

        // Only process left/right click actions.
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Optionally cancel the event so no default behavior interferes.
        event.setCancelled(true);

        // Determine which click type to register.
        String clickType = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) ? "L" : "R";
        castingManager.registerClick(player, clickType);
    }
} 