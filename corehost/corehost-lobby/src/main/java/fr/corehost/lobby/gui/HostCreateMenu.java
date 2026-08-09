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
import fr.corehost.lobby.utils.ItemBuilder;

import java.util.List;
import java.util.ArrayList;

public class HostCreateMenu implements CustomMenu {

    private final Inventory inventory;

    public HostCreateMenu() {
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Créer un Serveur");
        initializeItems();
    }

    private void initializeItems() {
        // Border decoration (unified Pink + Purple)
        LobbyMenuUtils.fillBorder(inventory);

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
                
                ItemBuilder builder = new ItemBuilder(mat)
                    .setName(ChatColor.translateAlternateColorCodes('&', name))
                    .addPersistentData(gameKey, PersistentDataType.STRING, gameId);
                    
                if (configLore != null) {
                    List<String> lore = new ArrayList<>();
                    for (String line : configLore) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    builder.setLore(lore);
                }
                
                ItemStack gameItem = builder.build();
                
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, gameItem);
                }
            }
        }
    }

    public void open(Player player) {
        if (player.hasMetadata("modmode")) {
            player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + ChatColor.RED + "Vous ne pouvez pas créer un host en mode Modération !");
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        if (player.hasMetadata("modmode")) {
            player.sendMessage(fr.corehost.lobby.utils.Constants.PREFIX + ChatColor.RED + "Vous ne pouvez pas créer un host en mode Modération !");
            player.closeInventory();
            return;
        }
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
                
                ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
                boolean hasSettings = false;
                
                if (gamesSection != null && gamesSection.contains(gameId)) {
                    hasSettings = gamesSection.getBoolean(gameId + ".has-settings-menu", false);
                    if (hasSettings) {
                        String className = gamesSection.getString(gameId + ".settings-menu-class");
                        if (className != null) {
                            try {
                                Class<?> clazz = Class.forName(className);
                                Object menu = clazz.getConstructor(CoreHostLobby.class).newInstance(plugin);
                                clazz.getMethod("open", Player.class).invoke(menu, player);
                            } catch (Exception e) {
                                plugin.getLogger().severe("Could not open settings menu for " + gameId + ": " + e.getMessage());
                                plugin.getCloudNetServiceManager().createHost(player, gameId, 3, false, false);
                            }
                        } else {
                            plugin.getCloudNetServiceManager().createHost(player, gameId, 3, false, false);
                        }
                    } else {
                        plugin.getCloudNetServiceManager().createHost(player, gameId, 3, false, false);
                    }
                } else {
                    plugin.getCloudNetServiceManager().createHost(player, gameId, 3, false, false);
                }
            }
        }
    }
}
