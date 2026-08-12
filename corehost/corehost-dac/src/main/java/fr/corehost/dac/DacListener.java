package fr.corehost.dac;

import fr.corehost.api.utils.CC;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;

import java.util.Optional;
@SuppressWarnings({"deprecation", "removal"})
public class DacListener implements Listener {

    private final CoreHostDac plugin;

    public DacListener(CoreHostDac plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        String prefix = plugin.getConfig().getString("gameplay.world-prefix", "dac");
        if (worldName.toLowerCase().startsWith(prefix.toLowerCase())) {
            String template = plugin.getConfig().getString("slimeworld.default-template", "default");
            plugin.getGameManager().createInstance(worldName, template);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        String prefix = plugin.getConfig().getString("gameplay.world-prefix", "dac");
        if (worldName.toLowerCase().startsWith(prefix.toLowerCase())) {
            DacGameInstance instance = plugin.getGameManager().getInstance(worldName);
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
        
        DacGameInstance instance = plugin.getGameManager().getInstance(worldName);
        if (instance != null) {
            instance.addPlayer(player);
        } else {
            boolean isDac = false;
            String prefix = plugin.getConfig().getString("gameplay.world-prefix", "dac");
            if (worldName.toLowerCase().contains(prefix.toLowerCase())) {
                isDac = true;
            } else {
                try {
                    fr.corehost.game.CoreHostGame coreGame = org.bukkit.plugin.java.JavaPlugin.getPlugin(fr.corehost.game.CoreHostGame.class);
                    if (coreGame != null && coreGame.getRedisManager() != null) {
                        fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(coreGame.getRedisManager());
                        java.util.UUID hostId = java.util.UUID.fromString(worldName);
                        fr.corehost.api.host.HostData data = hostManager.getHost(hostId).join();
                        if (data != null && prefix.equalsIgnoreCase(data.getGameType())) {
                            isDac = true;
                        }
                    }
                } catch (Exception e) {}
            }
            
            if (isDac) {
                String template = plugin.getConfig().getString("slimeworld.default-template", "default");
                instance = plugin.getGameManager().createInstance(worldName, template);
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
        String prefix = plugin.getConfig().getString("gameplay.world-prefix", "dac");
        if (event.getGameType().equalsIgnoreCase(prefix)) {
            DacMapConfig mapConfig = plugin.getMapManager().getRandomFunctionalMap();
            if (mapConfig != null) {
                event.setTemplateName(mapConfig.getTemplateName());
                plugin.getGameManager().setPendingMap(event.getHostId(), mapConfig.getName());
            } else {
                plugin.getLogger().severe("AUCUNE MAP DAC CONFIGURÉE ! La création de l'instance est annulée pour protéger le serveur.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Optional<DacGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(event.getPlayer());
        if (optInstance.isPresent()) {
            DacGameInstance instance = optInstance.get();
            Player p = event.getPlayer();
            
            if (instance.getState() == DacGameInstance.GameState.PLAYING) {
                Player current = instance.getCurrentPlayer();
                if (current != null && current.getUniqueId().equals(p.getUniqueId())) {
                    org.bukkit.block.Block feet = p.getLocation().getBlock();
                    org.bukkit.block.Block below = p.getLocation().subtract(0, 0.1, 0).getBlock();
                    
                    boolean nearPool = p.getLocation().getY() <= instance.getMapConfig().getPoolYLevel() + 3.0;
                    
                    if (feet.getType() == Material.WATER || (nearPool && (feet.getType().name().endsWith("WOOL") || feet.getType().name().endsWith("CONCRETE")))) {
                        instance.handleLanding(p, feet);
                    } else if (below.getType() == Material.WATER || (nearPool && (below.getType().name().endsWith("WOOL") || below.getType().name().endsWith("CONCRETE")))) {
                        instance.handleLanding(p, below);
                    } else if (p.getLocation().getY() < instance.getMapConfig().getPoolYLevel() - 2) {
                        // Completely missed the pool
                        instance.handleLanding(p, feet);
                    }
                } else if (instance.getMapConfig() != null) {
                    org.bukkit.block.Block feet = p.getLocation().getBlock();
                    org.bukkit.block.Block below = p.getLocation().subtract(0, 0.1, 0).getBlock();
                    boolean nearPool = p.getLocation().getY() <= instance.getMapConfig().getPoolYLevel() + 3.0;
                    
                    if (feet.getType() == Material.WATER || below.getType() == Material.WATER || 
                       (nearPool && (feet.getType().name().endsWith("WOOL") || feet.getType().name().endsWith("CONCRETE"))) ||
                       (nearPool && (below.getType().name().endsWith("WOOL") || below.getType().name().endsWith("CONCRETE")))) {
                        
                        org.bukkit.Location specSpawn = instance.getMapConfig().getSpectatorSpawn();
                        if (specSpawn != null) {
                            specSpawn = specSpawn.clone();
                            specSpawn.setWorld(instance.getWorld());
                        }
                        p.teleport(specSpawn != null ? specSpawn : instance.getWorld().getSpawnLocation());
                    }
                }
            }
            
            int y = event.getTo().getBlockY();
            if (y <= plugin.getConfig().getInt("gameplay.void-level", 50)) {
                if (instance.getState() == DacGameInstance.GameState.PLAYING) {
                    Player current = instance.getCurrentPlayer();
                    if (current != null && current.getUniqueId().equals(p.getUniqueId())) {
                        instance.handleLanding(p, p.getLocation().getBlock());
                    } else {
                        p.teleport(instance.getMapConfig().getSpectatorSpawn() != null ? instance.getMapConfig().getSpectatorSpawn() : instance.getWorld().getSpawnLocation());
                    }
                } else {
                    p.teleport(instance.getMapConfig().getSpectatorSpawn() != null ? instance.getMapConfig().getSpectatorSpawn() : instance.getWorld().getSpawnLocation());
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Optional<DacGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(player);
            
            if (optInstance.isPresent()) {
                DacGameInstance instance = optInstance.get();
                
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL && instance.getState() == DacGameInstance.GameState.PLAYING) {
                    Player current = instance.getCurrentPlayer();
                    if (current != null && current.getUniqueId().equals(player.getUniqueId())) {
                        org.bukkit.block.Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();
                        if (below.getType() == Material.AIR) {
                            below = player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                        }
                        instance.handleLanding(player, below);
                    }
                }
                
                // Cancel all damage in DAC
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Optional<DacGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer((Player) event.getEntity());
            if (optInstance.isPresent()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Optional<DacGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(event.getPlayer());
        if (optInstance.isPresent() && event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Optional<DacGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(event.getPlayer());
        if (optInstance.isPresent() && event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        Player player = event.getPlayer();
        org.bukkit.inventory.ItemStack item = event.getItem();
        
        if (item != null && item.getType() == Material.RED_BED && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().contains("Retour au Lobby")) {
                event.setCancelled(true);
                
                Optional<DacGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(player);
                if (optInstance.isPresent()) {
                    DacGameInstance instance = optInstance.get();
                    if (instance.getState() == DacGameInstance.GameState.WAITING || instance.getState() == DacGameInstance.GameState.STARTING) {
                        player.sendMessage(CC.GREEN + "Retour au lobby...");
                        try {
                            com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                            out.writeUTF("Connect");
                            out.writeUTF(plugin.getConfig().getString("bungeecord.fallback-server", "lobby"));
                            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                        } catch (Exception e) {
                            player.sendMessage(CC.RED + "Impossible de se connecter au lobby.");
                        }
                    }
                }
            }
        }
    }
}
