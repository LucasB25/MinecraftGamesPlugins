package fr.corehost.sumo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Optional;

public class SumoListener implements Listener {

    private final CoreHostSumo plugin;

    public SumoListener(CoreHostSumo plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // If the world is a sumo instance, we should initialize it.
        // For simplicity, let's assume world names starting with "sumo-" are sumo instances.
        // Or we can rely on Redis. We'll check if the world name is something we track.
        String worldName = event.getWorld().getName();
        if (worldName.toLowerCase().startsWith("sumo")) { // fallback check if Redis isn't used
            // We assume mapName is "sumo" for now, or extracted from worldName.
            // plugin.getGameManager().createInstance(worldName, "default");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        
        // If joining a sumo world, add them to the instance
        // Assuming hostId = worldName
        SumoGameInstance instance = plugin.getGameManager().getInstance(worldName);
        if (instance != null) {
            instance.addPlayer(player);
        } else {
            // Might be a dynamically loaded world that wasn't registered yet?
            // Let's create it on the fly for testing
            if (worldName.toLowerCase().contains("sumo")) {
                plugin.getGameManager().createInstance(worldName, "default");
                plugin.getGameManager().getInstance(worldName).addPlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(event.getPlayer());
        optInstance.ifPresent(instance -> instance.removePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(event.getPlayer());
        if (optInstance.isPresent()) {
            SumoGameInstance instance = optInstance.get();
            if (instance.getState() == SumoGameInstance.GameState.PLAYING) {
                int y = event.getTo().getBlockY();
                if (y <= instance.getMapConfig().getDeathHeight()) {
                    instance.handleDeath(event.getPlayer());
                }
            } else if (instance.getState() == SumoGameInstance.GameState.WAITING || instance.getState() == SumoGameInstance.GameState.STARTING) {
                // Prevent moving down or out of spawn area if needed
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(player);
            
            if (optInstance.isPresent()) {
                SumoGameInstance instance = optInstance.get();
                if (instance.getState() != SumoGameInstance.GameState.PLAYING) {
                    event.setCancelled(true);
                } else {
                    // Cancel actual damage but keep knockback (for Sumo)
                    event.setDamage(0);
                    
                    // Cancel fall damage entirely
                    if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
