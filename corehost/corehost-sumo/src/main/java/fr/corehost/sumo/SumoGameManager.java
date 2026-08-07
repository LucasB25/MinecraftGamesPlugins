package fr.corehost.sumo;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SumoGameManager {

    private final CoreHostSumo plugin;
    private final Map<String, SumoGameInstance> instances = new HashMap<>();
    private final Map<UUID, SumoGameInstance> playerInstances = new HashMap<>();

    public SumoGameManager(CoreHostSumo plugin) {
        this.plugin = plugin;
    }

    public synchronized SumoGameInstance createInstance(String hostId, String mapName) {
        if (instances.containsKey(hostId)) {
            return instances.get(hostId);
        }

        World world = Bukkit.getWorld(hostId);
        if (world == null) {
            plugin.getLogger().warning("World " + hostId + " is not loaded!");
            return null;
        }

        SumoMapConfig mapConfig = plugin.getMapManager().getRandomFunctionalMap();
        if (mapConfig == null) {
            plugin.getLogger().warning("Aucune carte fonctionnelle trouvée ! L'instance sera injouable.");
        }

        SumoGameInstance instance = new SumoGameInstance(plugin, hostId, world, mapConfig);
        instances.put(hostId, instance);
        plugin.getLogger().info("Created Sumo instance for host " + hostId + " on map " + mapName);
        return instance;
    }

    public void removeInstance(String hostId) {
        instances.remove(hostId);
    }

    public SumoGameInstance getInstance(String hostId) {
        return instances.get(hostId);
    }
    
    public SumoGameInstance getInstance(World world) {
        return getInstance(world.getName());
    }

    public Optional<SumoGameInstance> getInstanceForPlayer(Player player) {
        return Optional.ofNullable(playerInstances.get(player.getUniqueId()));
    }

    public void registerPlayer(UUID uuid, SumoGameInstance instance) {
        playerInstances.put(uuid, instance);
    }

    public void unregisterPlayer(UUID uuid) {
        playerInstances.remove(uuid);
    }
}
