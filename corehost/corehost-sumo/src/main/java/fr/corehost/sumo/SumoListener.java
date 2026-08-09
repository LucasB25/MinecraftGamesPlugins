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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.Sound;

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
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        Player player = event.getPlayer();
        org.bukkit.inventory.ItemStack item = event.getItem();
        
        if (item != null && item.getType() == org.bukkit.Material.RED_BED && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().contains("Retour au Lobby")) {
                event.setCancelled(true);
                
                Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(player);
                if (optInstance.isPresent()) {
                    SumoGameInstance instance = optInstance.get();
                    if (instance.getState() == SumoGameInstance.GameState.WAITING || instance.getState() == SumoGameInstance.GameState.STARTING) {
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "Retour au lobby...");
                        
                        try {
                            com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                            out.writeUTF("Connect");
                            out.writeUTF("lobby");
                            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                        } catch (Exception e) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Impossible de se connecter au lobby.");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPreSlimeCreate(fr.corehost.game.events.PreSlimeInstanceCreateEvent event) {
        if (event.getGameType().equalsIgnoreCase("sumo")) {
            SumoMapConfig mapConfig = plugin.getMapManager().getRandomFunctionalMap();
            if (mapConfig != null) {
                event.setTemplateName(mapConfig.getTemplateName());
                plugin.getGameManager().setPendingMap(event.getHostId(), mapConfig.getName());
            } else {
                plugin.getLogger().severe("AUCUNE MAP SUMO CONFIGURÉE ! Le système va générer un monde par défaut (Vanilla). Veuillez utiliser /sumosetup pour configurer une map !");
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
                    instance.handleDeath(event.getPlayer(), true);
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
                        instance.handleDeath(player, true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();
            
            Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(attacker);
            if (optInstance.isPresent()) {
                SumoGameInstance instance = optInstance.get();
                if (instance.getState() == SumoGameInstance.GameState.PLAYING && instance.hasPlayer(victim.getUniqueId())) {
                    
                    // Si la victime est dans sa période d'invulnérabilité (suite à un précédent coup), on ignore ce coup (anti-spam)
                    if (victim.getNoDamageTicks() > victim.getMaximumNoDamageTicks() / 2.0F) {
                        return;
                    }
                    
                    // Attacker gets +1 combo and +1 total hits
                    int currentCombo = instance.getCurrentCombos().getOrDefault(attacker.getUniqueId(), 0);
                    int newCombo = currentCombo + 1;
                    instance.getCurrentCombos().put(attacker.getUniqueId(), newCombo);
                    
                    int currentMax = instance.getMaxCombos().getOrDefault(attacker.getUniqueId(), 0);
                    if (newCombo > currentMax) {
                        instance.getMaxCombos().put(attacker.getUniqueId(), newCombo);
                    }
                    
                    int currentHits = instance.getTotalHits().getOrDefault(attacker.getUniqueId(), 0);
                    instance.getTotalHits().put(attacker.getUniqueId(), currentHits + 1);
                    
                    // Victim combo resets to 0
                    instance.getCurrentCombos().put(victim.getUniqueId(), 0);
                    
                    if (instance.isCustomKB()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            double h = plugin.getConfig().getDouble("custom-kb.horizontal", 0.45);
                            double v = plugin.getConfig().getDouble("custom-kb.vertical", 0.36);
                            double sprintMult = plugin.getConfig().getDouble("custom-kb.sprint-multiplier", 1.3);
                            double airMult = plugin.getConfig().getDouble("custom-kb.air-multiplier", 0.8);
                            double maxY = plugin.getConfig().getDouble("custom-kb.max-y", 0.4);
                            double friction = plugin.getConfig().getDouble("custom-kb.friction", 2.0);

                            org.bukkit.util.Vector direction = attacker.getLocation().getDirection().setY(0).normalize();
                            
                            if (attacker.isSprinting()) {
                                direction.multiply(sprintMult);
                            }
                            
                            direction.multiply(h);
                            
                            org.bukkit.util.Vector currentVel = victim.getVelocity();
                            
                            double finalX = (currentVel.getX() / friction) + direction.getX();
                            double finalZ = (currentVel.getZ() / friction) + direction.getZ();
                            
                            double finalY = (currentVel.getY() / friction) + v;
                            if (!victim.isOnGround()) {
                                finalY *= airMult;
                            }
                            
                            if (finalY > maxY) {
                                finalY = maxY;
                            }

                            victim.setVelocity(new org.bukkit.util.Vector(finalX, finalY, finalZ));
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        
        Optional<SumoGameInstance> optInstance = plugin.getGameManager().getInstanceForPlayer(player);
        if (optInstance.isPresent()) {
            SumoGameInstance instance = optInstance.get();
            if (instance.getState() == SumoGameInstance.GameState.PLAYING) {
                if (instance.isDoubleJumpEnabled() && !instance.getUsedDoubleJump().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    
                    instance.getUsedDoubleJump().add(player.getUniqueId());
                    
                    org.bukkit.util.Vector jump = player.getLocation().getDirection().multiply(0.5).setY(1.0);
                    player.setVelocity(jump);
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
                } else {
                    player.setAllowFlight(false);
                }
            } else {
                // If not playing, don't allow flight unless they are in creative
                event.setCancelled(true);
                player.setFlying(false);
                if (!instance.isDoubleJumpEnabled() || instance.getUsedDoubleJump().contains(player.getUniqueId())) {
                    player.setAllowFlight(false);
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
