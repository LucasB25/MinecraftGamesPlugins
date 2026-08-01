package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import fr.corehost.lobby.CoreHostLobby;

import java.util.List;
import java.util.ArrayList;

public class HostCreateMenu implements CustomMenu {

    private final Inventory inventory;

    public HostCreateMenu() {
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Créer un Serveur");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration
        ItemStack filler1 = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta1 = filler1.getItemMeta();
        if (meta1 != null) { meta1.setDisplayName(" "); filler1.setItemMeta(meta1); }
        
        ItemStack filler2 = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta2 = filler2.getItemMeta();
        if (meta2 != null) { meta2.setDisplayName(" "); filler2.setItemMeta(meta2); }
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 17 || i == 9 || i == 17) {
                inventory.setItem(i, (i % 2 == 0) ? filler1 : filler2);
            }
        }

        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
        
        if (gamesSection != null) {
            NamespacedKey gameKey = new NamespacedKey(plugin, "game_id");
            
            for (String gameId : gamesSection.getKeys(false)) {
                String name = gamesSection.getString(gameId + ".name", "&e" + gameId);
                String materialName = gamesSection.getString(gameId + ".material", "BEDROCK");
                int slot = gamesSection.getInt(gameId + ".slot", 0);
                List<String> configLore = gamesSection.getStringList(gameId + ".lore");
                
                Material mat = Material.matchMaterial(materialName);
                if (mat == null) mat = Material.BEDROCK;
                
                ItemStack gameItem = new ItemStack(mat);
                ItemMeta meta = gameItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                    
                    if (configLore != null) {
                        List<String> lore = new ArrayList<>();
                        for (String line : configLore) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                        meta.setLore(lore);
                    }
                    
                    meta.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, gameId);
                    gameItem.setItemMeta(meta);
                }
                
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, gameItem);
                }
            }
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        NamespacedKey gameKey = new NamespacedKey(plugin, "game_id");
        ItemMeta meta = clicked.getItemMeta();
        
        if (meta != null && meta.getPersistentDataContainer().has(gameKey, PersistentDataType.STRING)) {
            String gameId = meta.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
            if (gameId != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                player.closeInventory();
                
                if (gameId.equalsIgnoreCase("Sumo")) {
                    new SumoSettingsMenu(plugin).open(player);
                } else {
                    // Default to BO3 for other games, or ignore if not applicable
                    plugin.getCloudNetServiceManager().createHost(player, gameId, 3);
                }
            }
        }
    }
}
