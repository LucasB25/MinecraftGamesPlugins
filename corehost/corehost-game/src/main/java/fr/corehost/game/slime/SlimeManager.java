package fr.corehost.game.slime;

import fr.corehost.game.CoreHostGame;
import fr.corehost.api.redis.RedisManager;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

public class SlimeManager {

    private final CoreHostGame plugin;
    private final RedisManager redisManager;
    private final String serverName;

    public SlimeManager(CoreHostGame plugin, RedisManager redisManager, String serverName) {
        this.plugin = plugin;
        this.redisManager = redisManager;
        this.serverName = serverName;
    }

    public void loadWorld(String templateName, String worldName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Loading Slime world " + worldName + " from template " + templateName + "...");
                
                // TODO: Implémenter l'API AdvancedSlimePaper pour cloner 'templateName' vers 'worldName'
                // SlimePlugin slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
                // SlimeWorld clone = slimePlugin.readWorld(...).clone(worldName);
                // slimePlugin.generateWorld(clone);
                
                // Simulation of world loading
                Thread.sleep(100);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("World " + worldName + " loaded from template " + templateName + "!");
                    
                    // Send Redis PubSub message to proxy to teleport players
                    JsonObject response = new JsonObject();
                    response.addProperty("action", "HOST_READY");
                    response.addProperty("hostId", worldName);
                    response.addProperty("serverName", serverName);
                    redisManager.publish("corehost:proxy:events", response.toString());
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load slime world " + worldName + ": " + e.getMessage());
            }
        });
    }
}
