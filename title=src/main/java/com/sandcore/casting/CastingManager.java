package com.sandcore.casting;

import org.bukkit.BukkitTask;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CastingManager {
    private final JavaPlugin plugin;
    private final Map<UUID, StringBuilder> clickCombos = new HashMap<>();
    private final Map<UUID, BukkitTask> comboTasks = new HashMap<>();
    private final long comboTimeout = 100L; // 5 seconds

    public CastingManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerClick(Player player, String clickType) {
        UUID uuid = player.getUniqueId();
        StringBuilder combo = clickCombos.computeIfAbsent(uuid, k -> new StringBuilder());
        combo.append(clickType);

        String feedback = "Current Combo: " + combo.toString();
        player.sendActionBar(feedback);
        player.sendTitle("", feedback, 5, 20, 5);
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F);

        String comboStr = combo.toString();
        int count = comboStr.split(",").length;
        if (count == 3) {
            if (comboTasks.containsKey(uuid)) {
                comboTasks.get(uuid).cancel();
                comboTasks.remove(uuid);
 