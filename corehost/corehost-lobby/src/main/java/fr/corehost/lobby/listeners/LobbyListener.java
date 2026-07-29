package fr.corehost.lobby.listeners;

import fr.corehost.lobby.gui.CustomMenu;
import fr.corehost.lobby.gui.HostCreateMenu;
import fr.corehost.lobby.gui.HostSearchMenu;
import fr.corehost.lobby.gui.PlayerProfileMenu;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import fr.corehost.lobby.CoreHostLobby;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyListener implements Listener {

    public static final java.util.Set<UUID> pendingFriendAdd = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final CoreHostLobby plugin;

    public LobbyListener(CoreHostLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null); // Hide vanilla join message
        
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.setAllowFlight(false);
        player.setCollidable(false); // Disable player collision
        
        // Cache player for friends system if bypassing Proxy
        if (plugin.getFriendManager() != null) {
            plugin.getFriendManager().cachePlayer(player.getName(), player.getUniqueId());
            
            // Notify friends that the player has joined
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                java.util.Set<String> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
                for (String friendUuidStr : friends) {
                    try {
                        UUID friendUuid = UUID.fromString(friendUuidStr);
                        Player onlineFriend = Bukkit.getPlayer(friendUuid);
                        if (onlineFriend != null && onlineFriend.isOnline()) {
                            if (plugin.getFriendManager().areNotificationsEnabled(friendUuid)) {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    onlineFriend.sendMessage(ChatColor.DARK_GRAY + "► " + ChatColor.YELLOW + "Votre ami " + ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " vient de se connecter !");
                                });
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
        }

        // Slot 4: Play Menu (Compass)
        ItemStack searchHost = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchHost.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Jouer " + ChatColor.GRAY + "(Clic-Droit)");
            searchHost.setItemMeta(searchMeta);
        }
        player.getInventory().setItem(4, searchHost);

        // Slot 8: Profile
        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta profileMeta = (SkullMeta) profile.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setOwningPlayer(player);
            profileMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Mon Profil " + ChatColor.GRAY + "(Clic-Droit)");
            profile.setItemMeta(profileMeta);
        }
        player.getInventory().setItem(8, profile);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (AuthListener.isBlocked(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR || !event.getAction().name().contains("RIGHT")) {
            // Cancel physical block interactions (doors, trapdoors, farming, etc) if they right/left click a block
            if (event.getClickedBlock() != null) {
                event.setCancelled(true);
            }
            return;
        }
        
        if (item.getType() == Material.COMPASS || item.getType() == Material.PLAYER_HEAD) {
            event.setCancelled(true);
        }

        if (item.getType() == Material.COMPASS) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            new HostSearchMenu().open(player);
        } else if (item.getType() == Material.PLAYER_HEAD) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            new PlayerProfileMenu(plugin, player).open(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (AuthListener.isBlocked(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Always cancel clicks in lobby to prevent moving hotbar items
        event.setCancelled(true);

        // If clicking a custom menu, let it handle the logic
        if (event.getInventory().getHolder() instanceof CustomMenu) {
            CustomMenu customMenu = (CustomMenu) event.getInventory().getHolder();
            customMenu.onClick(event, player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Prevent drag-splitting items in the lobby
        if (event.getWhoClicked() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null); // Hide vanilla quit message
        Player player = event.getPlayer();
        if (plugin.getFriendManager() != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getFriendManager().updateLastSeen(player.getUniqueId());
            });
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        event.setDroppedExp(0);
        // Instant respawn on next tick
        Bukkit.getScheduler().runTask(plugin, () -> event.getEntity().spigot().respawn());
    }

    // --- NEW SECURITIES ---

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        event.setCancelled(true); // Prevents ice melting, coral dying
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getY() < 0) {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (pendingFriendAdd.contains(player.getUniqueId())) {
            event.setCancelled(true);
            pendingFriendAdd.remove(player.getUniqueId());
            String message = event.getMessage().trim();
            if (message.equalsIgnoreCase("annuler") || message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "Ajout d'ami annulé.");
            } else {
                // Execute the command synchronously
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.chat("/friend add " + message);
                });
            }
        }
    }
}
