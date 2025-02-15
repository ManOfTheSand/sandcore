package com.sandcore.classes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
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
 * It dynamically builds the interface using both class definitions (from classes.yml)
 * and GUI layout settings (from gui.yml). This allows you to customize the title, size,
 * background, and the positions where the dynamic class items will be placed.
 */
public class ClassSelectionGUI implements Listener {
    private static final String DEFAULT_TITLE = "&aClass Selection";
    private JavaPlugin plugin;
    private ClassManager classManager;
    
    public ClassSelectionGUI(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        // Register as listener for inventory click events.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the class selection GUI for the specified player, using settings from gui.yml.
     *
     * The configuration file (gui.yml) should be located in the plugin's data folder.
     * If it doesn't exist, you must first copy it from the jar.
     *
     * Expected configuration structure (example):
     *
     * gui:
     *   title: "&aCustom GUI Menu"
     *   rows: 3
     *   background:
     *     material: "BLACK_STAINED_GLASS_PANE"
     *     displayName: "&r"
     *     lore: []
     *   classStartSlot: 10
     *   classSlots: [12, 13, 14]   <-- (optional list of specific slots)
     *
     * @param player The player who will see the GUI.
     */
    public void openGUI(Player player) {
        // Load gui.yml from the plugin data folder.
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        // Read basic settings.
        String title = guiConfig.getString("gui.title", DEFAULT_TITLE);
        int rows = guiConfig.getInt("gui.rows", 3);
        int inventorySize = rows * 9;
        Inventory guiInventory = Bukkit.createInventory(null, inventorySize, ChatColor.translateAlternateColorCodes('&', title));
        
        // Setup background item.
        String bgMaterialName = guiConfig.getString("gui.background.material", "BLACK_STAINED_GLASS_PANE");
        Material bgMaterial;
        try {
            bgMaterial = Material.valueOf(bgMaterialName);
        } catch (IllegalArgumentException e) {
            bgMaterial = Material.BARRIER;
        }
        ItemStack bgItem = new ItemStack(bgMaterial);
        ItemMeta bgMeta = bgItem.getItemMeta();
        bgMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', guiConfig.getString("gui.background.displayName", "&r")));
        List<String> bgLore = guiConfig.getStringList("gui.background.lore");
        List<String> translatedBgLore = new ArrayList<>();
        for (String line : bgLore) {
            translatedBgLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        bgMeta.setLore(translatedBgLore);
        bgItem.setItemMeta(bgMeta);
        
        // Fill entire inventory with background.
        for (int i = 0; i < inventorySize; i++) {
            guiInventory.setItem(i, bgItem.clone());
        }
        
        // Place dynamic class items.
        Map<String, ClassDefinition> classDefs = classManager.getAllClasses();
        if (classDefs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No class definitions loaded. Check your classes.yml file.");
            plugin.getLogger().warning("ClassSelectionGUI: No class definitions found.");
            return;
        }
        
        // Option 1: Use specific classSlots if provided.
        List<Integer> classSlots = guiConfig.getIntegerList("gui.classSlots");
        if (classSlots != null && !classSlots.isEmpty() && classSlots.size() >= classDefs.size()) {
            int slotIndex = 0;
            for (Map.Entry<String, ClassDefinition> entry : classDefs.entrySet()) {
                int slot = classSlots.get(slotIndex);
                if (slot >= 0 && slot < inventorySize) {
                    setClassItem(guiInventory, entry.getValue(), slot);
                }
                slotIndex++;
            }
        } else {
            // Option 2: Use a starting slot from config or default to 10.
            int startSlot = guiConfig.getInt("gui.classStartSlot", 10);
            int i = 0;
            for (Map.Entry<String, ClassDefinition> entry : classDefs.entrySet()) {
                int slot = startSlot + i;
                if (slot < inventorySize) {
                    setClassItem(guiInventory, entry.getValue(), slot);
                }
                i++;
            }
        }
        
        // Optionally: You could load extra fixed items from a section like gui.fixedItems
        // and overlay them here if needed.
        
        player.openInventory(guiInventory);
        plugin.getLogger().info("Opened class selection GUI for " + player.getName() + " using gui.yml configuration.");
    }
    
    /**
     * Helper method to set a class item in the inventory at the specified slot.
     *
     * @param inventory The inventory to update.
     * @param def       The class definition.
     * @param slot      The slot to place the item.
     */
    private void setClassItem(Inventory inventory, ClassDefinition def, int slot) {
        Material material;
        try {
            material = Material.valueOf(def.getMaterial());
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER;
        }
        ItemStack classItem = new ItemStack(material);
        ItemMeta meta = classItem.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', def.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', def.getLore()));
        meta.setLore(lore);
        classItem.setItemMeta(meta);
        inventory.setItem(slot, classItem);
    }
    
    /**
     * Listens to inventory click events for the class selection GUI.
     * It cancels all item movements and processes the player's click to set the chosen class,
     * providing feedback and logging the selection.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String inventoryTitle = event.getView().getTitle();
        // Check if the inventory matches our GUI's title by stripping color codes.
        if (ChatColor.stripColor(inventoryTitle).equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', DEFAULT_TITLE)))
            || inventoryTitle.contains("Custom GUI Menu")) {
            event.setCancelled(true); // Lock the GUI.
            
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();
            
            List<ClassDefinition> classList = new ArrayList<>(classManager.getAllClasses().values());
            // Determine if the click corresponds to a class selection.
            // We check the dynamic slots; if slot is one of the registered ones in our config, then process it.
            int usedSlot = -1;
            // Check if a list of dedicated class slots was used.
            File guiFile = new File(plugin.getDataFolder(), "gui.yml");
            YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
            List<Integer> classSlots = guiConfig.getIntegerList("gui.classSlots");
            if (classSlots != null && !classSlots.isEmpty()) {
                if (classSlots.contains(slot)) {
                    usedSlot = classSlots.indexOf(slot);
                }
            } else {
                usedSlot = slot - guiConfig.getInt("gui.classStartSlot", 10);
            }
            
            if (usedSlot >= 0 && usedSlot < classList.size()) {
                ClassDefinition selected = classList.get(usedSlot);
                classManager.setPlayerClass(player, selected.getId());
                player.sendMessage(ChatColor.GREEN + "You have selected the " +
                        ChatColor.translateAlternateColorCodes('&', selected.getDisplayName()) + ChatColor.GREEN + " class!");
                plugin.getLogger().info("Player " + player.getName() + " selected class " + selected.getId());
                player.closeInventory();
            }
        }
    }
} 