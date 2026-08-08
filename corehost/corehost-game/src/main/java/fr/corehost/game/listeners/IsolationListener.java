package fr.corehost.game.listeners;

import fr.corehost.game.CoreHostGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;


public class IsolationListener implements Listener {

    private final CoreHostGame plugin;

    public IsolationListener(CoreHostGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoinHost(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 1. Check pending joins for this player
        String targetHostId = plugin.getPendingJoins().remove(player.getUniqueId());
        
        if (plugin.getRedisManager() != null) {
            try {
                fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(plugin.getRedisManager());
                
                if (targetHostId != null) {
                    // Player was explicitly sent here to join a specific host
                    java.util.UUID hId = java.util.UUID.fromString(targetHostId);
                    fr.corehost.api.host.HostData h = hostManager.getHost(hId);
                    if (h != null && h.getServerName().equalsIgnoreCase(plugin.getServerName())) {
                        org.bukkit.World w = Bukkit.getWorld(h.getWorldName());
                        if (w != null) {
                            player.teleport(w.getSpawnLocation());
                            return; // Stop checking further
                        }
                    }
                }
                
                // 2. Fallback: if they are the owner of an active host on this server
                for (fr.corehost.api.host.HostData h : hostManager.getAllHosts()) {
                    if (h.getServerName().equalsIgnoreCase(plugin.getServerName())) {
                        if (h.getOwnerUuid().equals(player.getUniqueId())) {
                            org.bukkit.World w = Bukkit.getWorld(h.getWorldName());
                            if (w != null) {
                                player.teleport(w.getSpawnLocation());
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        String joinedWorld = joined.getWorld().getName();

        // 1. Update visibility for all players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(joined)) continue;

            if (!online.getWorld().getName().equals(joinedWorld)) {
                // Not in the same world: hide each other
                online.hidePlayer(plugin, joined);
                joined.hidePlayer(plugin, online);
            } else {
                // In the same world: check spectator status
                boolean joinedIsSpec = plugin.getSpectatorManager() != null && plugin.getSpectatorManager().isSpectator(joined);
                boolean onlineIsSpec = plugin.getSpectatorManager() != null && plugin.getSpectatorManager().isSpectator(online);

                if (joinedIsSpec && !onlineIsSpec) {
                    if (joined.hasMetadata("vanished") && joined.getMetadata("vanished").get(0).asBoolean()) {
                        online.hidePlayer(plugin, joined);
                    } else {
                        online.showPlayer(plugin, joined); // Keep in tab
                        online.hideEntity(plugin, joined); // Hide model
                    }
                } else {
                    if (joined.hasMetadata("vanished") && joined.getMetadata("vanished").get(0).asBoolean()) {
                        online.hidePlayer(plugin, joined);
                    } else {
                        online.showPlayer(plugin, joined);
                        online.showEntity(plugin, joined);
                    }
                }

                if (onlineIsSpec && !joinedIsSpec) {
                    if (online.hasMetadata("vanished") && online.getMetadata("vanished").get(0).asBoolean()) {
                        joined.hidePlayer(plugin, online);
                    } else {
                        joined.showPlayer(plugin, online); // Keep in tab
                        joined.hideEntity(plugin, online); // Hide model
                    }
                } else {
                    if (online.hasMetadata("vanished") && online.getMetadata("vanished").get(0).asBoolean()) {
                        joined.hidePlayer(plugin, online);
                    } else {
                        joined.showPlayer(plugin, online);
                        joined.showEntity(plugin, online);
                    }
                }
            }
        }
        
        // Disable global join message
        event.setJoinMessage(null);
        
        // Disable collisions
        joined.setCollidable(false);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String newWorld = player.getWorld().getName();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;

            if (!online.getWorld().getName().equals(newWorld)) {
                online.hidePlayer(plugin, player);
                player.hidePlayer(plugin, online);
            } else {
                boolean playerIsSpec = plugin.getSpectatorManager() != null && plugin.getSpectatorManager().isSpectator(player);
                boolean onlineIsSpec = plugin.getSpectatorManager() != null && plugin.getSpectatorManager().isSpectator(online);

                if (playerIsSpec && !onlineIsSpec) {
                    if (player.hasMetadata("vanished") && player.getMetadata("vanished").get(0).asBoolean()) {
                        online.hidePlayer(plugin, player);
                    } else {
                        online.showPlayer(plugin, player); // Keep in tab
                        online.hideEntity(plugin, player); // Hide model
                    }
                } else {
                    if (player.hasMetadata("vanished") && player.getMetadata("vanished").get(0).asBoolean()) {
                        online.hidePlayer(plugin, player);
                    } else {
                        online.showPlayer(plugin, player);
                        online.showEntity(plugin, player);
                    }
                }

                if (onlineIsSpec && !playerIsSpec) {
                    if (online.hasMetadata("vanished") && online.getMetadata("vanished").get(0).asBoolean()) {
                        player.hidePlayer(plugin, online);
                    } else {
                        player.showPlayer(plugin, online); // Keep in tab
                        player.hideEntity(plugin, online); // Hide model
                    }
                } else {
                    if (online.hasMetadata("vanished") && online.getMetadata("vanished").get(0).asBoolean()) {
                        player.hidePlayer(plugin, online);
                    } else {
                        player.showPlayer(plugin, online);
                        player.showEntity(plugin, online);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Disable global quit message
        event.setQuitMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        
        Player sender = event.getPlayer();
        String worldName = sender.getWorld().getName();
        
        // Remove recipients that are not in the same world
        event.getRecipients().removeIf(recipient -> !recipient.getWorld().getName().equals(worldName));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        String worldName = victim.getWorld().getName();
        
        // If there's a death message, we only want players in this world to see it
        // We cancel the global one and broadcast manually to the world
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            for (Player player : victim.getWorld().getPlayers()) {
                player.sendMessage(deathMessage);
            }
            event.setDeathMessage(null);
        }
    }
}
