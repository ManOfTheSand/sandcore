package com.sandcore.listeners;

import java.util.logging.Logger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.sandcore.data.PlayerData;
import com.sandcore.data.PlayerDataManager;
import com.sandcore.hud.HUDManager;
import com.sandcore.levels.LevelManager;
import com.sandcore.levels.XPSourceManager;

/**
 * XPListener awards XP to players for killing mobs.
 * It listens for mob death events, retrieves the XP value from xp-sources.yml,
 * adds that XP to the player's persistent data, updates the player's HUD,
 * and logs the XP award and any level up.
 */
public class XPListener implements Listener {
    private final XPSourceManager xpSourceManager;
    private final PlayerDataManager playerDataManager;
    private final LevelManager levelManager;
    private final HUDManager hudManager;
    private final Logger logger;
    
    public XPListener(XPSourceManager xpSourceManager, PlayerDataManager playerDataManager,
                      LevelManager levelManager, HUDManager hudManager, Logger logger) {
        this.xpSourceManager = xpSourceManager;
        this.playerDataManager = playerDataManager;
        this.levelManager = levelManager;
        this.hudManager = hudManager;
        this.logger = logger;
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        // Only process if the entity is a living mob and has a killer.
        if (entity instanceof LivingEntity) {
            Player killer = ((LivingEntity) entity).getKiller();
            if (killer != null) {
                String mobType = entity.getType().name();
                int xpAward = xpSourceManager.getXPForMob(mobType);
                if (xpAward > 0) {
                    PlayerData data = playerDataManager.getPlayerData(killer.getUniqueId());
                    boolean leveledUp = data.addXP(xpAward, levelManager);
                    logger.info("Awarded " + xpAward + " XP to " + killer.getName() +
                            " for killing " + mobType + ". Total XP: " + data.getXP() + ", Level: " + data.getLevel());
                    hudManager.updateHUD(killer, data);
                    if (leveledUp) {
                        killer.sendMessage("§aCongratulations! You've reached level " + data.getLevel() + "!");
                        logger.info(killer.getName() + " leveled up to level " + data.getLevel());
                    }
                }
            }
        }
    }
} 