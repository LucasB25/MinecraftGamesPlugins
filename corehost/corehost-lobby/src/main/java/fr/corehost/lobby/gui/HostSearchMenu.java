package fr.corehost.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostStatus;

import java.util.List;

public class HostSearchMenu implements CustomMenu {

    private final Inventory inventory;

    public HostSearchMenu() {
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_AQUA + "Recherche de Host");
        initializeItems();
    }

    private void initializeItems() {
        // Bottom bar decoration
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Create Host item (Centered on last line: slot 49)
        ItemStack createItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Créer un Host");
            createMeta.setLore(java.util.Arrays.asList(
                "",
                ChatColor.GRAY + "Cliquez pour créer votre propre",
                ChatColor.GRAY + "serveur de jeu personnalisé !"
            ));
            createItem.setItemMeta(createMeta);
        }
        inventory.setItem(49, createItem);

        // Refresh item (Next to it: slot 50)
        ItemStack refreshItem = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Rafraîchir la liste");
            refreshMeta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Recharger la liste des serveurs",
                ChatColor.GRAY + "actuellement disponibles."
            ));
            refreshItem.setItemMeta(refreshMeta);
        }
        inventory.setItem(50, refreshItem);
        
        // Fetch running hosts from Redis
        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        List<HostData> hosts = plugin.getHostManager().getAllHosts();
        
        int slot = 0;
        NamespacedKey serverKey = new NamespacedKey(plugin, "server_name");

        for (HostData host : hosts) {
            if (slot >= 45) break; // Maximum capacity in this page

            Material mat = host.getGameType().equalsIgnoreCase("Sumo") ? Material.SLIME_BALL : Material.RED_BANNER;
            ItemStack hostItem = new ItemStack(mat);
            ItemMeta hostMeta = hostItem.getItemMeta();
            
            if (hostMeta != null) {
                hostMeta.setDisplayName(ChatColor.AQUA + "Serveur " + host.getGameType());
                
                String statusColor = host.getStatus() == HostStatus.PLAYING ? ChatColor.RED.toString() : ChatColor.GREEN.toString();
                
                hostMeta.setLore(java.util.Arrays.asList(
                    "",
                    ChatColor.GRAY + "Hôte : " + ChatColor.YELLOW + host.getOwnerName(),
                    ChatColor.GRAY + "Joueurs : " + ChatColor.YELLOW + host.getCurrentPlayers() + "/" + host.getMaxPlayers(),
                    ChatColor.GRAY + "Statut : " + statusColor + host.getStatus().name(),
                    "",
                    ChatColor.GREEN + "► Cliquez pour rejoindre"
                ));
                
                // Store the server name in the item's persistent data
                hostMeta.getPersistentDataContainer().set(serverKey, PersistentDataType.STRING, host.getServerName());
                
                hostItem.setItemMeta(hostMeta);
            }
            
            inventory.setItem(slot, hostItem);
            slot++;
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

        if (clicked.getType() == Material.EMERALD) {
            player.sendMessage(ChatColor.YELLOW + "Rafraîchissement de la liste des serveurs...");
            // Clear items and re-initialize
            for(int i = 0; i < 45; i++) {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
            initializeItems();
        } else if (clicked.getType() == Material.NETHER_STAR) {
            new HostCreateMenu().open(player);
        } else {
            // Check if it's a server item
            CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
            NamespacedKey serverKey = new NamespacedKey(plugin, "server_name");
            ItemMeta meta = clicked.getItemMeta();
            
            if (meta != null && meta.getPersistentDataContainer().has(serverKey, PersistentDataType.STRING)) {
                String serverName = meta.getPersistentDataContainer().get(serverKey, PersistentDataType.STRING);
                if (serverName != null) {
                    player.sendMessage(ChatColor.GREEN + "Connexion au serveur " + serverName + "...");
                    plugin.connectToServer(player, serverName);
                    player.closeInventory();
                }
            }
        }
    }
}
