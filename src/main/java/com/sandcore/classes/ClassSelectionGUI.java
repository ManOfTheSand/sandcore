package com.sandcore.classes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.sandcore.util.ColorUtils;

/**
 * ClassSelectionGUI constructs and manages the Class Selection GUI.
 * It reads its layout from gui.yml (from the "classGUI" section),
 * letting you configure the title, rows, background, dynamic class item slots,
 * and fixed items (like close or info buttons).
 */
public class ClassSelectionGUI implements Listener {
    private JavaPlugin plugin;
    private ClassManager classManager;
    private static final String DEFAULT_TITLE = "<hex:#CCCCCC>Class Selection";
    
    // Map to keep track of which inventory (GUI) has which dynamic class items.
    // Key: the Inventory instance; Value: mapping from slot number to ClassDefinition.
    private Map<Inventory, Map<Integer, ClassDefinition>> guiMappings = new HashMap<>();

    public ClassSelectionGUI(JavaPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        // Register this class as an event listener.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the Class Selection GUI for the specified player using settings from gui.yml.
     */
    public void openGUI(Player player) {
        // Load gui.yml from the plugin's data folder.
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        // Get the configuration section for the Class Selection GUI
        ConfigurationSection classGuiSection = guiConfig.getConfigurationSection("classGUI");
        if (classGuiSection == null) {
            plugin.getLogger().severe("classGUI section not found in gui.yml. Using default settings.");
        }
        
        // Read title, rows, and calculate inventory size.
        String title = (classGuiSection != null) ? classGuiSection.getString("title", DEFAULT_TITLE) : DEFAULT_TITLE;
        int rows = (classGuiSection != null) ? classGuiSection.getInt("rows", 3) : 3;
        int inventorySize = rows * 9;
        Inventory guiInventory = Bukkit.createInventory(null, inventorySize, ColorUtils.translate(title));
        
        // Setup background item from configuration.
        Material bgMaterial;
        if (classGuiSection != null) {
            String bgMaterialName = classGuiSection.getString("background.material", "GRAY_STAINED_GLASS_PANE");
            try {
                bgMaterial = Material.valueOf(bgMaterialName);
            } catch (IllegalArgumentException e) {
                bgMaterial = Material.BARRIER;
            }
        } else {
            bgMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack bgItem = new ItemStack(bgMaterial);
        ItemMeta bgMeta = bgItem.getItemMeta();
        String bgDisplay = (classGuiSection != null) ? classGuiSection.getString("background.displayName", "<hex:#FFFFFF>") : "<hex:#FFFFFF>";
        bgMeta.setDisplayName(ColorUtils.translate(bgDisplay));
        List<String> bgLore = (classGuiSection != null) ? classGuiSection.getStringList("background.lore") : new ArrayList<>();
        List<String> translatedBgLore = new ArrayList<>();
        for (String line : bgLore) {
            translatedBgLore.add(ColorUtils.translate(line));
        }
        bgMeta.setLore(translatedBgLore);
        bgItem.setItemMeta(bgMeta);
        // Fill all inventory slots with the background.
        for (int i = 0; i < inventorySize; i++) {
            guiInventory.setItem(i, bgItem.clone());
        }
        
        // Place dynamic class items using the slot defined in each class configuration.
        Map<String, ClassDefinition> classDefs = classManager.getAllClasses();
        if (classDefs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No class definitions loaded. Check your classes.yml file.");
            plugin.getLogger().warning("ClassSelectionGUI: No class definitions found.");
            return;
        }
        
        int defaultSlot = (classGuiSection != null) ? classGuiSection.getInt("classStartSlot", 10) : 10;
        int counter = 0;
        // Build a mapping of slot -> ClassDefinition for this GUI.
        Map<Integer, ClassDefinition> mapping = new HashMap<>();
        for (Map.Entry<String, ClassDefinition> entry : classDefs.entrySet()) {
            int slot = entry.getValue().getSlot();
            if (slot < 0 || slot >= inventorySize) {
                // Fallback: assign sequentially starting at defaultSlot.
                slot = defaultSlot + counter;
                counter++;
            }
            if (slot < inventorySize) {
                setClassItem(guiInventory, entry.getValue(), slot);
                mapping.put(slot, entry.getValue());
            }
        }
        // Store the mapping for this GUI.
        guiMappings.put(guiInventory, mapping);
        
        // Add fixed items defined in the configuration.
        if (classGuiSection != null) {
            ConfigurationSection fixedItemsSection = classGuiSection.getConfigurationSection("fixedItems");
            if (fixedItemsSection != null) {
                for (String key : fixedItemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = fixedItemsSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        int slot = itemSection.getInt("slot", -1);
                        if (slot >= 0 && slot < inventorySize) {
                            String materialName = itemSection.getString("material", "BARRIER");
                            Material material;
                            try {
                                material = Material.valueOf(materialName);
                            } catch (IllegalArgumentException e) {
                                material = Material.BARRIER;
                            }
                            ItemStack fixedItem = new ItemStack(material);
                            ItemMeta meta = fixedItem.getItemMeta();
                            String displayName = itemSection.getString("displayName", "");
                            meta.setDisplayName(ColorUtils.translate(displayName));
                            List<String> lore = itemSection.getStringList("lore");
                            List<String> translatedLore = new ArrayList<>();
                            for (String line : lore) {
                                translatedLore.add(ColorUtils.translate(line));
                            }
                            meta.setLore(translatedLore);
                            fixedItem.setItemMeta(meta);
                            guiInventory.setItem(slot, fixedItem);
                        }
                    }
                }
            }
        }
        
        player.openInventory(guiInventory);
        plugin.getLogger().info("Opened class selection GUI for " + player.getName() + " using gui.yml configuration.");
    }
    
    /**
     * Helper method to place a dynamic class item in the inventory at the specified slot.
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
        meta.setDisplayName(ColorUtils.translate(def.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.translate(def.getLore()));
        meta.setLore(lore);
        classItem.setItemMeta(meta);
        inventory.setItem(slot, classItem);
    }
    
    /**
     * Listens to inventory click events. If a dynamic class item is clicked,
     * the player's class is set accordingly.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Ensure we only process clicks in the top inventory.
        if (event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        String inventoryTitle = event.getView().getTitle();
        // Identify this GUI by checking for "Class Selection" in the stripped title.
        if (!ChatColor.stripColor(inventoryTitle).contains("Class Selection")) {
            return;
        }
        event.setCancelled(true); // Lock the GUI so items cannot be moved.
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Look up the mapping for this GUI.
        Map<Integer, ClassDefinition> mapping = guiMappings.get(event.getInventory());
        if (mapping == null) {
            return;
        }

        // If the clicked slot corresponds to a class, process the selection.
        if (!mapping.containsKey(slot)) {
            return;
        }

        ClassDefinition selected = mapping.get(slot);
        classManager.setPlayerClass(player, selected.getId());
        player.sendMessage(ColorUtils.translate("<hex:#00FF00>You have selected the " + selected.getDisplayName() + " class!"));
        plugin.getLogger().info("Player " + player.getName() + " selected class " + selected.getId());
        player.closeInventory();
        // Optionally, remove the mapping for this closed inventory.
        guiMappings.remove(event.getInventory());
    }
} 