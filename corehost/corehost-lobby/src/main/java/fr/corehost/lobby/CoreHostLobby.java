package fr.corehost.lobby;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import fr.corehost.api.redis.RedisManager;
import fr.corehost.api.host.HostManager;
import fr.corehost.lobby.cloudnet.CloudNetServiceManager;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

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
            if (this.redisManager.isConnected()) {
                this.hostManager = new HostManager(this.redisManager);
                getLogger().info("Connected to Redis successfully.");
            } else {
                getLogger().warning("Redis is not reachable at " + redisHost + ":" + redisPort + ". Host features are disabled.");
                this.redisManager = null;
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Redis: " + e.getMessage());
            this.redisManager = null;
        }
        
        // Initialize CloudNet Manager
        this.cloudNetServiceManager = new CloudNetServiceManager(this);

        // Register BungeeCord channel for server switching
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register Listeners
        getServer().getPluginManager().registerEvents(new fr.corehost.lobby.listeners.LobbyListener(), this);

        // Register Commands
        fr.corehost.lobby.commands.SpawnCommand spawnCommand = new fr.corehost.lobby.commands.SpawnCommand();
        if (getCommand("spawn") != null) getCommand("spawn").setExecutor(spawnCommand);
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

    public void connectToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }
}
