package fr.corehost.lobby;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.GameRule;
import org.bukkit.plugin.PluginManager;
import fr.corehost.lobby.listeners.*;
import fr.corehost.api.redis.RedisManager;
import fr.corehost.api.host.HostManager;
import fr.corehost.api.friends.FriendManager;
import fr.corehost.api.party.PartyManager;
import fr.corehost.lobby.cloudnet.CloudNetServiceManager;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class CoreHostLobby extends JavaPlugin {

    private RedisManager redisManager;
    private HostManager hostManager;
    private FriendManager friendManager;
    private PartyManager partyManager;
    private CloudNetServiceManager cloudNetServiceManager;
    private fr.corehost.api.database.DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        getLogger().info("CoreHostLobby is starting...");

        // Load configuration
        saveDefaultConfig();
        fr.corehost.lobby.utils.Constants.load(getConfig());
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        
        String dbHost = getConfig().getString("database.host", "127.0.0.1");
        int dbPort = getConfig().getInt("database.port", 3306);
        String dbName = getConfig().getString("database.database", "corehost");
        String dbUser = getConfig().getString("database.user", "root");
        String dbPassword = getConfig().getString("database.password", "");

        // Connect to Redis and Database
        try {
            this.redisManager = new RedisManager(redisHost, redisPort, redisPassword);
            if (this.redisManager.isConnected()) {
                this.databaseManager = new fr.corehost.api.database.DatabaseManager(dbHost, dbPort, dbName, dbUser, dbPassword);
                this.hostManager = new HostManager(this.redisManager);
                this.friendManager = new FriendManager(this.redisManager, this.databaseManager);
                this.partyManager = new PartyManager(this.redisManager);
                getLogger().info("Connected to Redis and Database successfully.");
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
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new fr.corehost.lobby.listeners.LobbyListener(this), this);
        pm.registerEvents(new AuthListener(this), this);

        // Register Commands
        fr.corehost.lobby.commands.SpawnCommand spawnCommand = new fr.corehost.lobby.commands.SpawnCommand();
        if (getCommand("spawn") != null) getCommand("spawn").setExecutor(spawnCommand);
        
        fr.corehost.lobby.commands.PremiumExceptionCommand premiumCommand = new fr.corehost.lobby.commands.PremiumExceptionCommand(this);
        if (getCommand("premiumexception") != null) getCommand("premiumexception").setExecutor(premiumCommand);

        fr.corehost.lobby.commands.CoreHostCommand coreHostCommand = new fr.corehost.lobby.commands.CoreHostCommand(this);
        if (getCommand("corehost") != null) getCommand("corehost").setExecutor(coreHostCommand);


        
        // Setup Worlds Security (Delayed by 1 tick to ensure worlds are fully loaded)
        getServer().getScheduler().runTask(this, this::setupWorlds);
    }

    @SuppressWarnings("deprecation")
    private void setupWorlds() {
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            world.setGameRule(GameRule.LOCATOR_BAR, false);
            world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
            world.setGameRule(GameRule.DO_TILE_DROPS, false);
            world.setGameRule(GameRule.DO_FIRE_TICK, false);
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            world.setGameRule(GameRule.SPAWN_RADIUS, 0);
            world.setGameRule(GameRule.DISABLE_RAIDS, true);
            world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
            world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
            world.setGameRule(GameRule.MOB_GRIEFING, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_MOB_LOOT, false);
            world.setGameRule(GameRule.DO_INSOMNIA, false);
            world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
            
            try {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    world.setTime(6000L); // Noon
                    world.setStorm(false);
                }
            } catch (Exception e) {
                getLogger().warning("Impossible de définir l'heure/météo pour le monde " + world.getName() + " : " + e.getMessage());
            }
        }
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

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
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
