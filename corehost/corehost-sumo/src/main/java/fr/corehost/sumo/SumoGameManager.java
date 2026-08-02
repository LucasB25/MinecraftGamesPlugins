package fr.corehost.sumo;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SumoGameManager {

    private final CoreHostSumo plugin;
    private final Map<String, SumoGameInstance> instances = new HashMap<>();

    public SumoGameManager(CoreHostSumo plugin) {
        this.plugin = plugin;
    }

    public void createInstance(String hostId, String mapName) {
        World world = Bukkit.getWorld(hostId);
        if (world == null) {
            plugin.getLogger().warning("World " + hostId + " is not loaded!");
            return;
        }

        SumoMapConfig mapConfig = plugin.getMapManager().getRandomFunctionalMap();
        if (mapConfig == null) {
            plugin.getLogger().warning("Aucune carte fonctionnelle trouvée ! L'instance sera injouable.");
        }

        SumoGameInstance instance = new SumoGameInstance(plugin, hostId, world, mapConfig);
        instances.put(hostId, instance);
        plugin.getLogger().info("Created Sumo instance for host " + hostId + " on map " + mapName);
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
        return instances.values().stream()
                .filter(instance -> instance.hasPlayer(player.getUniqueId()))
                .findFirst();
    }
}
