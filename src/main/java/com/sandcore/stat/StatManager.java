package com.sandcore.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.sandcore.SandCore;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class StatManager {
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final SandCore plugin;
    private final FileConfiguration config;

    public StatManager(SandCore plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadAttributeFormulas();
    }

    private final Map<String, String> formulas = new HashMap<>();

    private void loadAttributeFormulas() {
        ConfigurationSection secondary = config.getConfigurationSection("attributes.secondary");
        for (String key : secondary.getKeys(false)) {
            formulas.put(key, secondary.getString(key + ".formula"));
        }
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStats(player));
    }

    public class PlayerStats {
        private final Player player;
        private final Map<String, Double> attributes = new HashMap<>();
        private final Map<String, Integer> allocatedPoints = new HashMap<>();

        public PlayerStats(Player player) {
            this.player = player;
            calculateStats();
        }

        public void calculateStats() {
            // Calculate primary attributes
            ConfigurationSection primary = config.getConfigurationSection("attributes.primary");
            for (String attr : primary.getKeys(false)) {
                double base = primary.getDouble(attr + ".base");
                double perLevel = primary.getDouble(attr + ".per-level");
                int allocated = allocatedPoints.getOrDefault(attr, 0);
                attributes.put(attr, base + (player.getLevel() * perLevel) + allocated);
            }

            // Calculate secondary attributes
            for (Map.Entry<String, String> entry : formulas.entrySet()) {
                double value = evaluateFormula(entry.getValue());
                attributes.put(entry.getKey(), value);
            }
        }

        private double evaluateFormula(String formula) {
            try {
                Expression expression = new ExpressionBuilder(formula)
                    .variables(attributes.keySet())
                    .build()
                    .setVariables(attributes);
                return expression.evaluate();
            } catch (Exception e) {
                plugin.getLogger().severe("Error evaluating formula: " + formula);
                return 0.0;
            }
        }

        public double getAttribute(String key) {
            return attributes.getOrDefault(key, 0.0);
        }

        public Set<String> getAttributeNames() {
            return attributes.keySet();
        }
        
        public Map<String, Double> getAttributesMap() {
            return new HashMap<>(attributes);
        }

        public void increaseAttribute(String attribute, int points) {
            allocatedPoints.put(attribute, allocatedPoints.getOrDefault(attribute, 0) + points);
            calculateStats(); // Recalculate with new points
        }

        public Map<String, Integer> getAllocatedPoints() {
            return new HashMap<>(allocatedPoints);
        }
    }
} 