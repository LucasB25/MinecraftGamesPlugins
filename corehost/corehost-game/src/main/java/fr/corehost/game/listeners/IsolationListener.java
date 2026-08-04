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
                // In the same world: show each other
                online.showPlayer(plugin, joined);
                joined.showPlayer(plugin, online);
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
                online.showPlayer(plugin, player);
                player.showPlayer(plugin, online);
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
