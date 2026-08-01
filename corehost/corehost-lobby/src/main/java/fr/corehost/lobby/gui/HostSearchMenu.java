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
import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.api.host.HostData;
import fr.corehost.api.host.HostStatus;
import fr.corehost.lobby.utils.Constants;

import java.util.List;
import java.util.ArrayList;
import org.bukkit.configuration.ConfigurationSection;

public class HostSearchMenu implements CustomMenu {

    private final Inventory inventory;
    private String gameFilter = "ALL";
    private HostStatus statusFilter = null;

    public HostSearchMenu() {
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "» " + ChatColor.DARK_AQUA + "Recherche de Serveurs");
        initializeItems();
    }

    private void initializeItems() {
        // Bottom bar decoration
        drawBottomBar();
        drawHosts();
    }
    
    private void drawBottomBar() {
        ItemStack filler1 = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta1 = filler1.getItemMeta();
        if (meta1 != null) { meta1.setDisplayName(" "); filler1.setItemMeta(meta1); }
        
        ItemStack filler2 = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta2 = filler2.getItemMeta();
        if (meta2 != null) { meta2.setDisplayName(" "); filler2.setItemMeta(meta2); }
        
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, (i % 2 == 0) ? filler1 : filler2);
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
        
        // Filter by Game item (Slot 45)
        ItemStack gameFilterItem = new ItemStack(Material.HOPPER);
        ItemMeta gameFilterMeta = gameFilterItem.getItemMeta();
        if (gameFilterMeta != null) {
            gameFilterMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Filtre par Jeu");
            gameFilterMeta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Actuel : " + ChatColor.YELLOW + (gameFilter.equals("ALL") ? "Tous" : gameFilter),
                "",
                ChatColor.GREEN + "► Clic-Gauche : Suivant",
                ChatColor.RED + "► Clic-Droit : Précédent"
            ));
            gameFilterItem.setItemMeta(gameFilterMeta);
        }
        inventory.setItem(45, gameFilterItem);
        
        // Filter by Status item (Slot 46)
        ItemStack statusFilterItem = new ItemStack(Material.COMPARATOR);
        ItemMeta statusFilterMeta = statusFilterItem.getItemMeta();
        if (statusFilterMeta != null) {
            statusFilterMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Filtre par Statut");
            statusFilterMeta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Actuel : " + ChatColor.YELLOW + (statusFilter == null ? "Tous" : statusFilter.name()),
                "",
                ChatColor.GREEN + "► Clic-Gauche : Suivant",
                ChatColor.RED + "► Clic-Droit : Précédent"
            ));
            statusFilterItem.setItemMeta(statusFilterMeta);
        }
        inventory.setItem(46, statusFilterItem);
    }
    
    private void drawHosts() {
        // Clear host slots
        for(int i = 0; i < 45; i++) {
            inventory.setItem(i, new ItemStack(Material.AIR));
        }
        
        // Fetch running hosts from Redis
        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        
        if (plugin.getHostManager() == null) {
            ItemStack maintenanceItem = new ItemStack(Material.BARRIER);
            ItemMeta maintenanceMeta = maintenanceItem.getItemMeta();
            if (maintenanceMeta != null) {
                maintenanceMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Système en Maintenance");
                maintenanceMeta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Le système de Host est actuellement",
                    ChatColor.GRAY + "en cours de maintenance ou de",
                    ChatColor.GRAY + "mise à jour. Réessayez plus tard."
                ));
                maintenanceItem.setItemMeta(maintenanceMeta);
            }
            inventory.setItem(22, maintenanceItem); // Center of the inventory
            return;
        }
        
        List<HostData> hosts = plugin.getHostManager().getAllHosts();
        
        int slot = 0;
        NamespacedKey serverKey = new NamespacedKey(plugin, "server_name");

        for (HostData host : hosts) {
            if (slot >= 45) break; // Maximum capacity in this page
            
            // Apply Filters
            if (!gameFilter.equals("ALL") && !host.getGameType().equalsIgnoreCase(gameFilter)) {
                continue;
            }
            if (statusFilter != null && host.getStatus() != statusFilter) {
                continue;
            }

            String materialName = plugin.getConfig().getString("games." + host.getGameType() + ".material", "BEDROCK");
            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.BEDROCK;
            
            ItemStack hostItem = new ItemStack(mat);
            ItemMeta hostMeta = hostItem.getItemMeta();
            
            if (hostMeta != null) {
                hostMeta.setDisplayName(ChatColor.YELLOW + "Serveur " + host.getGameType());
                
                String statusColor = host.getStatus() == HostStatus.PLAYING ? ChatColor.RED.toString() : ChatColor.GREEN.toString();
                
                hostMeta.setLore(java.util.Arrays.asList(
                    "",
                    ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Hôte : " + ChatColor.WHITE + host.getOwnerName(),
                    ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Statut : " + statusColor + host.getStatus().name(),
                    ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Joueurs : " + ChatColor.YELLOW + host.getCurrentPlayers() + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + host.getMaxPlayers(),
                    "",
                    ChatColor.GREEN + "► Cliquez pour rejoindre !"
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

    private long lastActionTime = 0;

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String prefix = Constants.PREFIX;
        long currentTime = System.currentTimeMillis();

        if (clicked.getType() == Material.EMERALD) {
            if (currentTime - lastActionTime < 500) return;
            lastActionTime = currentTime;
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            player.sendMessage(prefix + "Rafraîchissement de la liste des serveurs...");
            drawHosts();
        } else if (clicked.getType() == Material.NETHER_STAR) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            new HostCreateMenu().open(player);
        } else if (clicked.getType() == Material.HOPPER && event.getSlot() == 45) {
            if (currentTime - lastActionTime < 500) return;
            lastActionTime = currentTime;
            
            // Cycle Game Filter
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            cycleGameFilter(event.getClick().isLeftClick());
            drawBottomBar();
            drawHosts();
        } else if (clicked.getType() == Material.COMPARATOR && event.getSlot() == 46) {
            if (currentTime - lastActionTime < 500) return;
            lastActionTime = currentTime;
            
            // Cycle Status Filter
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            cycleStatusFilter(event.getClick().isLeftClick());
            drawBottomBar();
            drawHosts();
        } else {
            // Check if it's a server item
            CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
            NamespacedKey serverKey = new NamespacedKey(plugin, "server_name");
            ItemMeta meta = clicked.getItemMeta();
            
            if (meta != null && meta.getPersistentDataContainer().has(serverKey, PersistentDataType.STRING)) {
                String serverName = meta.getPersistentDataContainer().get(serverKey, PersistentDataType.STRING);
                if (serverName != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.sendMessage(prefix + "Connexion au serveur " + ChatColor.GREEN + serverName + ChatColor.GRAY + "...");
                    plugin.connectToServer(player, serverName);
                    player.closeInventory();
                }
            }
        }
    }
    
    private void cycleGameFilter(boolean forward) {
        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
        
        List<String> availableGames = new ArrayList<>();
        availableGames.add("ALL");
        
        if (gamesSection != null) {
            availableGames.addAll(gamesSection.getKeys(false));
        }
        
        int currentIndex = availableGames.indexOf(gameFilter);
        
        if (forward) {
            if (currentIndex == -1 || currentIndex == availableGames.size() - 1) {
                gameFilter = "ALL";
            } else {
                gameFilter = availableGames.get(currentIndex + 1);
            }
        } else {
            if (currentIndex <= 0) {
                gameFilter = availableGames.get(availableGames.size() - 1);
            } else {
                gameFilter = availableGames.get(currentIndex - 1);
            }
        }
    }
    
    private void cycleStatusFilter(boolean forward) {
        if (forward) {
            if (statusFilter == null) {
                statusFilter = HostStatus.WAITING;
            } else if (statusFilter == HostStatus.WAITING) {
                statusFilter = HostStatus.STARTING;
            } else if (statusFilter == HostStatus.STARTING) {
                statusFilter = HostStatus.PLAYING;
            } else {
                statusFilter = null;
            }
        } else {
            if (statusFilter == null) {
                statusFilter = HostStatus.PLAYING;
            } else if (statusFilter == HostStatus.PLAYING) {
                statusFilter = HostStatus.STARTING;
            } else if (statusFilter == HostStatus.STARTING) {
                statusFilter = HostStatus.WAITING;
            } else {
                statusFilter = null;
            }
        }
    }
}
