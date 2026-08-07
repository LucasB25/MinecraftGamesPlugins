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
import fr.corehost.lobby.utils.ItemBuilder;

import java.util.List;
import java.util.ArrayList;
import org.bukkit.configuration.ConfigurationSection;

public class HostSearchMenu implements CustomMenu {

    private final Inventory inventory;
    private String gameFilter = "ALL";
    private HostStatus statusFilter = null;
    private org.bukkit.scheduler.BukkitTask refreshTask;

    public HostSearchMenu() {
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Recherche de Serveurs");
        initializeItems();
    }

    private void initializeItems() {
        // Bottom bar decoration
        drawBottomBar();
        drawHosts();
    }
    
    private void drawBottomBar() {
        LobbyMenuUtils.fillBottomRow(inventory);

        // Create Host item
        String currentFilter = gameFilter.equals("ALL") ? "Tous" : gameFilter;
        String currentStatusFilter = statusFilter == null ? "Tous" : statusFilter.name();

        ItemStack createItem = new ItemBuilder(Material.NETHER_STAR)
            .setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Créer un Host")
            .setLore(
                "",
                ChatColor.GRAY + "Créez votre propre serveur",
                ChatColor.GRAY + "et invitez vos amis !",
                "",
                ChatColor.YELLOW + "► Cliquez pour créer"
            ).build();
        inventory.setItem(53, createItem);

        // Refresh item
        ItemStack refreshItem = new ItemBuilder(Material.EMERALD)
            .setName(ChatColor.GREEN + "" + ChatColor.BOLD + "Rafraîchir la liste")
            .setLore(
                ChatColor.GRAY + "Mettre à jour les serveurs",
                ChatColor.DARK_GRAY + "(Actualisation automatique toutes les 2s)"
            ).build();
        inventory.setItem(49, refreshItem);
        
        // Filter by Game item
        ItemStack gameFilterItem = new ItemBuilder(Material.HOPPER)
            .setName(ChatColor.AQUA + "" + ChatColor.BOLD + "Filtre par Jeu")
            .setLore(
                ChatColor.GRAY + "Actuel: " + ChatColor.YELLOW + currentFilter,
                "",
                ChatColor.YELLOW + "► Cliquez pour changer"
            ).build();
        inventory.setItem(45, gameFilterItem);
        
        // Filter by Status item
        ItemStack statusFilterItem = new ItemBuilder(Material.COMPARATOR)
            .setName(ChatColor.AQUA + "" + ChatColor.BOLD + "Filtre par Statut")
            .setLore(
                ChatColor.GRAY + "Actuel: " + ChatColor.YELLOW + currentStatusFilter,
                "",
                ChatColor.YELLOW + "► Cliquez pour changer"
            ).build();
        inventory.setItem(46, statusFilterItem);
    }
    
    private void drawHosts() {
        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        
        if (plugin.getHostManager() == null) {
            // Clear host slots
            for(int i = 0; i < 45; i++) {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
            ItemStack maintenanceItem = new ItemBuilder(Material.BARRIER)
                .setName(ChatColor.RED + "" + ChatColor.BOLD + "Système en Maintenance")
                .setLore(
                    "",
                    ChatColor.GRAY + "La recherche de Host est",
                    ChatColor.GRAY + "temporairement désactivée."
                ).build();
            inventory.setItem(22, maintenanceItem); // Center of the inventory
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<HostData> hosts = plugin.getHostManager().getAllHosts();
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory.getViewers().isEmpty()) return;
                
                // Clear host slots
                for(int i = 0; i < 45; i++) {
                    inventory.setItem(i, new ItemStack(Material.AIR));
                }

                int index = 0;
                NamespacedKey hostKey = new NamespacedKey(plugin, "host_id");
                NamespacedKey serverKey = new NamespacedKey(plugin, "server_name");

                for (HostData host : hosts) {
                    if (index >= 45) break; // Maximum capacity in this page
                    
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
                    
                    String statusColor = host.getStatus() == HostStatus.PLAYING ? ChatColor.RED.toString() : ChatColor.GREEN.toString();
                    List<String> lore = java.util.Arrays.asList(
                            "",
                            ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Hôte : " + ChatColor.WHITE + host.getOwnerName(),
                            ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Statut : " + statusColor + host.getStatus().name(),
                            ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Joueurs : " + ChatColor.YELLOW + host.getCurrentPlayers() + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + host.getMaxPlayers(),
                            "",
                            ChatColor.GREEN + "► Cliquez pour rejoindre !"
                    );
                    
                    ItemStack hostItem = new ItemBuilder(mat)
                        .setName(ChatColor.YELLOW + "Serveur " + host.getGameType())
                        .setLore(lore)
                        .addPersistentData(hostKey, PersistentDataType.STRING, host.getHostId().toString())
                        .addPersistentData(serverKey, PersistentDataType.STRING, host.getServerName())
                        .build();
                    
                    inventory.setItem(index, hostItem);
                    index++;
                }
            });
        });
    }

    public void open(Player player) {
        player.openInventory(inventory);
        
        CoreHostLobby plugin = JavaPlugin.getPlugin(CoreHostLobby.class);
        this.refreshTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                    drawHosts();
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
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
            if (player.hasMetadata("modmode")) {
                player.sendMessage(prefix + ChatColor.RED + "Vous ne pouvez pas créer un host en mode Modération !");
                player.closeInventory();
                return;
            }
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
                    if (player.hasMetadata("modmode")) {
                        player.sendMessage(prefix + ChatColor.RED + "Vous ne pouvez pas rejoindre un host en mode Modération !");
                        player.closeInventory();
                        return;
                    }
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
