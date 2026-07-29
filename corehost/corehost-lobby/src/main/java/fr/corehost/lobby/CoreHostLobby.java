package fr.corehost.lobby;

import org.bukkit.plugin.java.JavaPlugin;
import fr.corehost.api.redis.RedisManager;

public class CoreHostLobby extends JavaPlugin {

    private RedisManager redisManager;

    @Override
    public void onEnable() {
        getLogger().info("CoreHostLobby is starting...");

        // Connect to Redis (you'll usually put this in a config.yml)
        try {
            this.redisManager = new RedisManager("localhost", 6379, "");
            getLogger().info("Connected to Redis.");
        } catch (Exception e) {
            getLogger().severe("Failed to connect to Redis!");
        }
        
        // TODO: Register Commands & Listeners
        // TODO: CloudNet warm pool API usage (or it's handled on CloudNet side via Tasks)
    }

    @Override
    public void onDisable() {
        if (redisManager != null) {
            redisManager.close();
        }
        getLogger().info("CoreHostLobby stopped.");
    }
    
    public RedisManager getRedisManager() {
        return redisManager;
    }
}
