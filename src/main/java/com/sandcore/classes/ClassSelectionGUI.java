package com.sandcore.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ClassSelectionGUI constructs and manages the GUI for class selection.
 * It dynamically builds the interface from class definitions, locks the GUI,
 * and listens for player clicks to set the player's class.
 */
public class ClassSelectionGUI implements Listener {
    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8Class Selection");
    private JavaPlugin plugin;
    private ClassManager classManager;

    public ClassSelectionGUI(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        // Register this class as a listener to handle inventory click events.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the class selection GUI for the specified player.
     *
     * @param player The player who will see the GUI.
     */
    public void openGUI(Player player) {
        Map<String, ClassDefinition> classes = classManager.getAllClasses();
        plugin.getLogger().info("ClassSelectionGUI: Found " + classes.size() + " class definitions.");

        if (classes.isEmpty()) {
            // Notify the player for debugging.
            player.sendMessage(ChatColor.RED + "No class definitions loaded. Check your classes.yml file.");
            return;
        }

        // Create an inventory with 9 slots (modify size if you expect more classes)
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);
        int slot = 0;
        for (Map.Entry<String, ClassDefinition> entry : classes.entrySet()) {
            ClassDefinition def = entry.getValue();
            Material material;
            try {
                // Convert the material name (from config) into a Bukkit Material.
                material = Material.valueOf(def.getMaterial());
            } catch (IllegalArgumentException e) {
                // If the provided material is invalid, fallback to a barrier.
                material = Material.BARRIER;
            }
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            // Translate hex color codes in the display name.
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', def.getDisplayName()));
            // Set the class's description/lore.
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', def.getLore()));
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }
        // Open the created GUI for the player.
        player.openInventory(gui);
        plugin.getLogger().info("Opened class selection GUI for " + player.getName());
    }

    /**
     * Listens to inventory click events for the class selection GUI. It cancels all item movements
     * and processes the player's click, setting the chosen class and providing feedback.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true); // Lock the GUI.

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            // Retrieve class definitions in the order they were added.
            List<ClassDefinition> classList = new ArrayList<>(classManager.getAllClasses().values());
            if (slot < classList.size()) {
                ClassDefinition selected = classList.get(slot);
                // Save the player's selected class.
                classManager.setPlayerClass(player, selected.getId());
                // Provide immediate feedback.
                player.sendMessage(ChatColor.GREEN + "You have selected the " +
                        ChatColor.translateAlternateColorCodes('&', selected.getDisplayName()) + ChatColor.GREEN + " class!");
                plugin.getLogger().info("Player " + player.getName() + " selected class " + selected.getId());
                // Close the GUI.
                player.closeInventory();
            }
        }
    }
} 