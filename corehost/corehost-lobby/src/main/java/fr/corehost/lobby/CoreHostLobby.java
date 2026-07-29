package fr.corehost.lobby;

import org.bukkit.plugin.java.JavaPlugin;
import fr.corehost.api.redis.RedisManager;
import fr.corehost.api.host.HostManager;
import fr.corehost.lobby.cloudnet.CloudNetServiceManager;

public class CoreHostLobby extends JavaPlugin {

    private RedisManager redisManager;
    private HostManager hostManager;
    private CloudNetServiceManager cloudNetServiceManager;

    @Override
    public void onEnable() {
        getLogger().info("CoreHostLobby is starting...");

        // Load configuration
        saveDefaultConfig();
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");

        // Connect to Redis
        try {
            this.redisManager = new RedisManager(redisHost, redisPort, redisPassword);
            this.hostManager = new HostManager(this.redisManager);
            getLogger().info("Connected to Redis.");
        } catch (Exception e) {
            getLogger().severe("Failed to connect to Redis!");
        }
        
        // Initialize CloudNet Manager
        this.cloudNetServiceManager = new CloudNetServiceManager(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new fr.corehost.lobby.listeners.LobbyListener(), this);
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

    public HostManager getHostManager() {
        return hostManager;
    }

    public CloudNetServiceManager getCloudNetServiceManager() {
        return cloudNetServiceManager;
    }
}
