package fr.corehost.dac;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DacGameManager {

    private final CoreHostDac plugin;
    private final Map<String, DacGameInstance> instances = new HashMap<>();
    private final Map<UUID, DacGameInstance> playerInstances = new HashMap<>();
    private final Map<String, String> pendingMaps = new HashMap<>();

    public DacGameManager(CoreHostDac plugin) {
        this.plugin = plugin;
    }

    public void setPendingMap(String hostId, String mapName) {
        pendingMaps.put(hostId, mapName);
    }

    public synchronized DacGameInstance createInstance(String hostId, String mapName) {
        if (instances.containsKey(hostId)) {
            return instances.get(hostId);
        }

        World world = Bukkit.getWorld(hostId);
        if (world == null) {
            plugin.getLogger().warning("World " + hostId + " is not loaded!");
            return null;
        }

        String pending = pendingMaps.remove(hostId);
        String actualMapName = (pending != null) ? pending : mapName;

        DacMapConfig mapConfig = plugin.getMapManager().getMap(actualMapName);
        if (mapConfig == null) {
            mapConfig = plugin.getMapManager().getRandomFunctionalMap();
        }
        
        if (mapConfig == null) {
            plugin.getLogger().warning("Aucune carte fonctionnelle trouvée ! L'instance sera injouable.");
        } else if (mapConfig.getSpectatorSpawn() != null) {
            org.bukkit.Location s1 = mapConfig.getSpectatorSpawn();
            world.setSpawnLocation(s1.getBlockX(), s1.getBlockY(), s1.getBlockZ(), s1.getYaw());
        }

        DacGameInstance instance = new DacGameInstance(plugin, hostId, world, mapConfig);
        instances.put(hostId, instance);
        plugin.getLogger().info("Created DAC instance for host " + hostId + " on map " + actualMapName);
        return instance;
    }

    public synchronized void removeInstance(String hostId) {
        instances.remove(hostId);
    }

    public synchronized void cleanupInstance(String hostId) {
        DacGameInstance instance = instances.get(hostId);
        if (instance != null) {
            World world = instance.getWorld();
            
            // Unregister all players
            for (UUID uuid : new java.util.ArrayList<>(instance.getPlayers())) {
                unregisterPlayer(uuid);
            }
            
            instances.remove(hostId);
            
            if (world != null && !world.getName().equalsIgnoreCase("dac")) {
                // Teleport remaining players in the world to the main world
                World mainWorld = Bukkit.getWorlds().get(0);
                for (Player p : world.getPlayers()) {
                    p.teleport(mainWorld.getSpawnLocation());
                }
                
                // Unload and delete world async
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.unloadWorld(world, false);
                    
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        // Supprimer le dossier classique si c'est un monde normal
                        java.io.File worldFolder = world.getWorldFolder();
                        try {
                            if (worldFolder.exists()) {
                                java.nio.file.Files.walk(worldFolder.toPath())
                                    .sorted(java.util.Comparator.reverseOrder())
                                    .map(java.nio.file.Path::toFile)
                                    .forEach(java.io.File::delete);
                            }
                        } catch (java.io.IOException e) {
                            plugin.getLogger().severe("Failed to delete world folder " + hostId + ": " + e.getMessage());
                        }
                        
                        // Supprimer le fichier slime si c'est un SlimeWorld
                        java.io.File slimeFile = new java.io.File(Bukkit.getWorldContainer(), "slime_worlds/" + hostId + ".slime");
                        if (slimeFile.exists()) {
                            new org.bukkit.scheduler.BukkitRunnable() {
                                int attempts = 0;
                                @Override
                                public void run() {
                                    if (!slimeFile.exists()) {
                                        cancel();
                                        return;
                                    }
                                    if (slimeFile.delete() || attempts >= 10) {
                                        cancel();
                                    }
                                    attempts++;
                                }
                            }.runTaskTimerAsynchronously(plugin, 10L, 20L);
                        }
                        
                        plugin.getLogger().info("Deleted world " + hostId);
                    });
                }, 20L); // Wait 1 second before unloading to ensure players are teleported
            }
        }
    }

    public DacGameInstance getInstance(String hostId) {
        return instances.get(hostId);
    }
    
    public java.util.Collection<DacGameInstance> getActiveInstances() {
        return instances.values();
    }
    
    public DacGameInstance getInstance(World world) {
        return getInstance(world.getName());
    }

    public Optional<DacGameInstance> getInstanceForPlayer(Player player) {
        return Optional.ofNullable(playerInstances.get(player.getUniqueId()));
    }

    public void registerPlayer(UUID uuid, DacGameInstance instance) {
        playerInstances.put(uuid, instance);
    }

    public void unregisterPlayer(UUID uuid) {
        playerInstances.remove(uuid);
    }
}
