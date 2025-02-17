package com.sandcore.damage;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import com.sandcore.SandCore;
import com.sandcore.stat.StatManager;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class DamageEngine implements Listener {
    private final SandCore plugin;
    private final StatManager statManager;

    public DamageEngine(SandCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            handlePlayerAttack((Player) event.getDamager(), event.getEntity(), event);
        }
    }

    private void handlePlayerAttack(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setCancelled(true);
        
        StatManager.PlayerStats stats = statManager.getPlayerStats(attacker);
        double damage = calculateDamage(stats, "physical");
        boolean isCrit = isCritical(stats);
        
        if (isCrit) {
            damage *= stats.getAttribute("critical_damage");
            showCriticalEffect(attacker);
        }
        
        applyDamage(attacker, victim, damage, DamageType.PHYSICAL);
    }

    private double calculateDamage(StatManager.PlayerStats stats, String damageType) {
        String formula = plugin.getConfig().getString("damage.formulas." + damageType + ".base");
        return new ExpressionBuilder(formula)
            .variables(stats.getAttributeNames())
            .build()
            .setVariables(stats.getAttributesMap())
            .evaluate();
    }

    private boolean isCritical(StatManager.PlayerStats stats) {
        double chance = stats.getAttribute("critical_chance") / 100.0;
        return Math.random() < chance;
    }

    private void applyDamage(Entity damager, Entity victim, double damage, DamageType type) {
        // Defense calculations
        double defense = getDefense(victim, type);
        double penetration = getPenetration(damager, type);
        double mitigated = damage * (1 - Math.max(0, defense - penetration));
        
        if (victim instanceof Damageable) {
            ((Damageable) victim).damage(mitigated);
            showDamageIndicator(victim, mitigated);
        }
    }

    private double getDefense(Entity entity, DamageType type) {
        if (entity instanceof Player) {
            return statManager.getPlayerStats((Player) entity).getAttribute(type + "_defense");
        }
        return plugin.getConfig().getDouble("damage.defense." + type + "_mob_base", 10.0);
    }

    private double getPenetration(Entity damager, DamageType type) {
        if (damager instanceof Player) {
            return statManager.getPlayerStats((Player) damager).getAttribute(type + "_pen");
        }
        return 0.0;
    }

    private enum DamageType {
        PHYSICAL, MAGICAL
    }

    private void showCriticalEffect(Player player) {
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 30);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }

    private void showDamageIndicator(Entity entity, double damage) {
        if (entity instanceof Player) {
            ((Player) entity).sendTitle("", "§c-" + String.format("%.1f", damage), 5, 15, 5);
        }
    }
} 