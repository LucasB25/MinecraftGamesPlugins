package fr.corehost.sumo;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

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
        if (worldName.toLowerCase().startsWith("sumo")) {
            // Create instance immediately so spawn location is set BEFORE players teleport
            plugin.getGameManager().createInstance(worldName, "default");
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        if (worldName.toLowerCase().startsWith("sumo")) {
            SumoGameInstance instance = plugin.getGameManager().getInstance(worldName);
            if (instance != null) {
                instance.addPlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setCollidable(false);
        String worldName = player.getWorld().getName();
        
        // If joining a sumo world, add them to the instance
        // Assuming hostId = worldName
        SumoGameInstance instance = plugin.getGameManager().getInstance(worldName);
        if (instance != null) {
            instance.addPlayer(player);
        } else {
            // Might be a dynamically loaded world that wasn't registered yet?
            boolean isSumo = false;
            
            if (worldName.toLowerCase().contains("sumo")) {
                isSumo = true;
            } else {
                try {
                    fr.corehost.game.CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(fr.corehost.game.CoreHostGame.class);
                    if (coreGame != null && coreGame.getRedisManager() != null) {
                        fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(coreGame.getRedisManager());
                        java.util.UUID hostId = java.util.UUID.fromString(worldName);
                        fr.corehost.api.host.HostData data = hostManager.getHost(hostId);
                        if (data != null && "sumo".equalsIgnoreCase(data.getGameType())) {
                            isSumo = true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore, not a valid UUID or redis is offline
                }
            }
            
            if (isSumo) {
                instance = plugin.getGameManager().createInstance(worldName, "default");
                if (instance != null) {
                    instance.addPlayer(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGameManager().getInstanceForPlayer(event.getPlayer()).ifPresent(instance -> {
            instance.removePlayer(event.getPlayer());
        });
    }

    @EventHandler
    public void onPreSlimeCreate(fr.corehost.game.events.PreSlimeInstanceCreateEvent event) {
        if (event.getGameType().equalsIgnoreCase("sumo")) {
            SumoMapConfig mapConfig = plugin.getMapManager().getRandomFunctionalMap();
            if (mapConfig != null) {
                event.setTemplateName(mapConfig.getTemplateName());
                plugin.getGameManager().setPendingMap(event.getHostId(), mapConfig.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(event.getPlayer());
        if (optInstance.isPresent()) {
            SumoGameInstance instance = optInstance.get();
            
            if (instance.isFrozen()) {
                org.bukkit.Location from = event.getFrom();
                org.bukkit.Location to = event.getTo();
                if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY())) {
                    org.bukkit.Location newTo = to.clone();
                    newTo.setX(from.getX());
                    newTo.setZ(from.getZ());
                    
                    // Empêcher de sauter (Y augmente), mais permettre de tomber (Y diminue)
                    if (to.getY() > from.getY()) {
                        newTo.setY(from.getY());
                    }
                    
                    event.setTo(newTo);
                    return;
                }
            }
            
            int y = event.getTo().getBlockY();
            if (y <= instance.getMapConfig().getDeathHeight()) {
                if (instance.getState() == SumoGameInstance.GameState.PLAYING) {
                    instance.handleDeath(event.getPlayer());
                } else if (instance.getState() == SumoGameInstance.GameState.WAITING || instance.getState() == SumoGameInstance.GameState.STARTING || instance.getState() == SumoGameInstance.GameState.ENDED) {
                    event.getPlayer().teleport(event.getPlayer().getWorld().getSpawnLocation());
                }
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
                    } else if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                        event.setCancelled(true);
                        instance.handleDeath(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer((Player) event.getEntity());
            if (optInstance.isPresent()) {
                event.setCancelled(true);
            }
        }
    }
}
