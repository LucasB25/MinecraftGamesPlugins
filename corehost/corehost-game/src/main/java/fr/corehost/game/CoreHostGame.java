package fr.corehost.game;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import fr.corehost.game.spectator.SpectatorManager;
import fr.corehost.game.spectator.SpectatorListener;

public class CoreHostGame extends JavaPlugin {
    
    private Logger log;
    private fr.corehost.api.redis.RedisManager redisManager;
    private fr.corehost.game.redis.GamePubSubListener pubSubListener;
    private SpectatorManager spectatorManager;
    private String serverName;
    private java.util.Map<java.util.UUID, String> pendingJoins = new java.util.concurrent.ConcurrentHashMap<>();
    
    @Override
    public void onEnable() {
        this.log = getLogger();
        log.info("CoreHostGame est en cours d'activation...");
        
        // Load config for Redis (Assuming we can create a default or load it from config.yml)
        saveDefaultConfig();
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        
        this.redisManager = new fr.corehost.api.redis.RedisManager(redisHost, redisPort, redisPassword);
        
        // Get CloudNet Service Name
        try {
            serverName = System.getenv("CLOUDNET_SERVICE_NAME");
            if (serverName == null || serverName.isEmpty()) {
                serverName = "Sumo-1";
            }
        } catch (Exception e) {
            serverName = "Sumo-1";
            log.warning("Impossible d'obtenir le nom CloudNet, utilisation de " + serverName);
        }
        
        // Initialiser SlimeManager
        fr.corehost.game.slime.SlimeManager slimeManager = new fr.corehost.game.slime.SlimeManager(this, redisManager, serverName);
        
        // Initialiser PubSub Listener
        this.pubSubListener = new fr.corehost.game.redis.GamePubSubListener(this, slimeManager, redisManager, serverName);
        redisManager.subscribe(pubSubListener, "corehost:game:" + serverName);
        
        // Initialiser SpectatorManager
        this.spectatorManager = new SpectatorManager(this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this, spectatorManager), this);
        
        // Initialiser IsolationListener
        getServer().getPluginManager().registerEvents(new fr.corehost.game.listeners.IsolationListener(this), this);
        
        // Register BungeeCord channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        // Register commands
        fr.corehost.game.commands.CoreHostGameCommand command = new fr.corehost.game.commands.CoreHostGameCommand(this);
        if (getCommand("corehostgame") != null) getCommand("corehostgame").setExecutor(command);
        
        log.info("CoreHostGame activé avec succès sur " + serverName + " !");
    }
    
    @Override
    public void onDisable() {
        if (pubSubListener != null) {
            pubSubListener.unsubscribe();
        }
        if (redisManager != null) {
            redisManager.close();
        }
        log.info("CoreHostGame désactivé.");
    }
    
    public fr.corehost.api.redis.RedisManager getRedisManager() {
        return redisManager;
    }
    
    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public java.util.Map<java.util.UUID, String> getPendingJoins() {
        return pendingJoins;
    }
}
