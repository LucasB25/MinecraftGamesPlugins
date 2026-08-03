package fr.corehost.lobby.listeners;

import fr.corehost.lobby.gui.CustomMenu;
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
import fr.corehost.lobby.utils.Constants;
import org.bukkit.Bukkit;

import java.util.UUID;

public class LobbyListener implements Listener {

    public static final java.util.Set<UUID> pendingFriendAdd = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    public static final java.util.Set<UUID> pendingPartyInvite = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
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
        
        // Precise spawn
        org.bukkit.Location spawn = player.getWorld().getSpawnLocation().clone();
        spawn.setX(spawn.getBlockX() + 0.5);
        spawn.setZ(spawn.getBlockZ() + 0.5);
        spawn.setYaw(spawn.getYaw() + 180f);
        player.teleport(spawn);
        
        // Cache player for friends system if bypassing Proxy
        if (plugin.getFriendManager() != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getFriendManager().cachePlayer(player.getName(), player.getUniqueId());
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
        // Scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().setupScoreboard(player);
        }

        // HeadHunt Cache
        if (plugin.getHeadHuntManager() != null) {
            plugin.getHeadHuntManager().loadPlayerCache(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (AuthListener.isBlocked(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(player.getUniqueId())) {
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

        if (fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(player.getUniqueId())) {
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
            Player player = (Player) event.getWhoClicked();
            if (!fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
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
        pendingFriendAdd.remove(player.getUniqueId());
        pendingPartyInvite.remove(player.getUniqueId());
        
        if (plugin.getParkourManager() != null) {
            plugin.getParkourManager().cancelParkour(player);
        }
        
        if (plugin.getFriendManager() != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getFriendManager().updateLastSeen(player.getUniqueId());
            });
        }
        
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeScoreboard(player);
        }
        
        if (plugin.getHeadHuntManager() != null) {
            plugin.getHeadHuntManager().unloadPlayerCache(player.getUniqueId());
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
    public void onEntitySpawn(org.bukkit.event.entity.EntitySpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity && !(event.getEntity() instanceof Player) && !(event.getEntity() instanceof org.bukkit.entity.ArmorStand)) {
            // Allow custom spawned entities, block natural spawns
            if (event.getEntity().getEntitySpawnReason() != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player) && !(entity instanceof org.bukkit.entity.ArmorStand)) {
                // If it's a mob that was saved in the world, remove it
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!fr.corehost.lobby.commands.AdminCommand.buildModePlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getY() < 0) {
            org.bukkit.Location spawn = player.getWorld().getSpawnLocation().clone();
            spawn.setX(spawn.getBlockX() + 0.5);
            spawn.setZ(spawn.getBlockZ() + 0.5);
            spawn.setYaw(spawn.getYaw() + 180f);
            player.teleport(spawn);
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
                String prefix = Constants.BUNGEE_PREFIX;
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour envoyer une demande d'ami à " + net.md_5.bungee.api.ChatColor.YELLOW + message + net.md_5.bungee.api.ChatColor.GRAY + " !");
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/friend add " + message));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.GREEN + "Cliquez pour ajouter")));
                player.spigot().sendMessage(msg);
            }
            return;
        }

        if (pendingPartyInvite.contains(player.getUniqueId())) {
            event.setCancelled(true);
            pendingPartyInvite.remove(player.getUniqueId());
            String message = event.getMessage().trim();
            if (message.equalsIgnoreCase("annuler") || message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "Invitation annulée.");
            } else {
                String prefix = Constants.BUNGEE_PREFIX;
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix + "Cliquez ici pour inviter " + net.md_5.bungee.api.ChatColor.YELLOW + message + net.md_5.bungee.api.ChatColor.GRAY + " dans votre groupe !");
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/party invite " + message));
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.GREEN + "Cliquez pour inviter")));
                player.spigot().sendMessage(msg);
            }
        }
    }
}
