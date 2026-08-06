package fr.corehost.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import fr.corehost.api.redis.RedisManager;
import fr.corehost.api.host.HostManager;
import fr.corehost.api.friends.FriendManager;
import fr.corehost.api.party.PartyManager;
import fr.corehost.proxy.config.ProxyConfig;
import fr.corehost.proxy.commands.FriendCommand;
import fr.corehost.proxy.commands.PartyCommand;
import fr.corehost.proxy.commands.MsgCommand;
import fr.corehost.proxy.commands.ReplyCommand;
import fr.corehost.proxy.commands.IgnoreCommand;
import fr.corehost.proxy.messages.MessageManager;
import fr.corehost.proxy.listeners.PlayerConnectionListener;
import fr.corehost.proxy.listeners.PartyListener;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

@Plugin(
    id = "corehost-proxy",
    name = "CoreHostProxy",
    version = "1.0-SNAPSHOT",
    authors = {"CoreHost"}
)
public class CoreHostProxy {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private RedisManager redisManager;
    private HostManager hostManager;
    private FriendManager friendManager;
    private PartyManager partyManager;
    private fr.corehost.api.profile.ProfileManager profileManager;
    private fr.corehost.api.database.DatabaseManager databaseManager;
    private fr.corehost.proxy.auth.AuthManager authManager;
    private fr.corehost.proxy.discord.DiscordManager discordManager;
    private fr.corehost.proxy.redis.ProxyPubSubListener proxyPubSubListener;
    private MessageManager messageManager;
    private ProxyConfig config;

    @Inject
    public CoreHostProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("CoreHostProxy is starting...");
        this.config = loadConfig();

        try {
            this.redisManager = new RedisManager(config.getRedisHost(), config.getRedisPort(), config.getRedisPassword());
            this.messageManager = new MessageManager(this);
            this.hostManager = new HostManager(this.redisManager);
            
            // Initialiser la base de données
            this.databaseManager = new fr.corehost.api.database.DatabaseManager(
                    config.getDbHost(), config.getDbPort(), config.getDbDatabase(),
                    config.getDbUser(), config.getDbPassword()
            );
            
            this.profileManager = new fr.corehost.api.profile.ProfileManager(this.databaseManager, this.redisManager, java.util.logging.Logger.getLogger("ProfileManager"));
            
            this.friendManager = new FriendManager(this.redisManager, this.databaseManager, this.profileManager);
            this.partyManager = new PartyManager(this.redisManager);
            
            // Run Data Migration
            fr.corehost.api.database.DatabaseMigration.migrateFromRedis(this.redisManager, this.databaseManager);
            
            logger.info("Connected to Redis and Database successfully.");
            
            this.proxyPubSubListener = new fr.corehost.proxy.redis.ProxyPubSubListener(this, server);
            this.redisManager.subscribe(proxyPubSubListener, "corehost:proxy:events");
            
            // Register Listener
            server.getEventManager().register(this, new PlayerConnectionListener(this));
            server.getEventManager().register(this, new PartyListener(this));
            
            // Register CloudNet
            try {
                Class.forName("eu.cloudnetservice.driver.inject.InjectionLayer");
                registerCloudNet();
            } catch (Throwable t) {
                logger.warn("CloudNet is not present or could not register CloudNetListener. Hosts will not auto-teleport.");
            }

            
            // Register Auth & Discord
            this.authManager = new fr.corehost.proxy.auth.AuthManager(this);
            server.getEventManager().register(this, authManager);
            
            this.discordManager = new fr.corehost.proxy.discord.DiscordManager(this);
            discordManager.start();
            
            // Register Command
            server.getCommandManager().register("friend", new FriendCommand(this, server), "f", "amie", "amis");
            server.getCommandManager().register("party", new PartyCommand(this, server), "p", "groupe");
            server.getCommandManager().register("msg", new MsgCommand(this, server), "w", "tell", "m");
            server.getCommandManager().register("reply", new ReplyCommand(this, server), "r", "rep");
            server.getCommandManager().register("ignore", new IgnoreCommand(this, server));
            server.getCommandManager().register("corehostproxy", new fr.corehost.proxy.commands.CoreHostProxyCommand(this));
            
        } catch (Exception e) {
            logger.error("An error occurred during CoreHostProxy initialization", e);
        }
    }

    private ProxyConfig loadConfig() {
        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

        if (!configFile.exists()) {
            try (java.io.InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, configFile.toPath());
                } else {
                    logger.error("Default config.yml not found in resources!");
                }
            } catch (IOException e) {
                logger.error("Could not create default config.yml", e);
            }
            return new ProxyConfig();
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                java.util.Map<String, Object> data = yaml.load(reader);
                ProxyConfig config = new ProxyConfig();
                
                if (data.containsKey("redis")) {
                    java.util.Map<String, Object> redisData = (java.util.Map<String, Object>) data.get("redis");
                    if (redisData.containsKey("host")) config.setRedisHost(String.valueOf(redisData.get("host")));
                    if (redisData.containsKey("port")) config.setRedisPort(Integer.parseInt(String.valueOf(redisData.get("port"))));
                    if (redisData.containsKey("password")) config.setRedisPassword(String.valueOf(redisData.get("password")));
                }
                
                if (data.containsKey("database")) {
                    java.util.Map<String, Object> dbData = (java.util.Map<String, Object>) data.get("database");
                    if (dbData.containsKey("host")) config.setDbHost(String.valueOf(dbData.get("host")));
                    if (dbData.containsKey("port")) config.setDbPort(Integer.parseInt(String.valueOf(dbData.get("port"))));
                    if (dbData.containsKey("database")) config.setDbDatabase(String.valueOf(dbData.get("database")));
                    if (dbData.containsKey("user")) config.setDbUser(String.valueOf(dbData.get("user")));
                    if (dbData.containsKey("password")) config.setDbPassword(String.valueOf(dbData.get("password")));
                }
                
                if (data.containsKey("discord")) {
                    java.util.Map<String, Object> discordData = (java.util.Map<String, Object>) data.get("discord");
                    if (discordData.containsKey("bot-token")) config.setDiscordBotToken(String.valueOf(discordData.get("bot-token")));
                    if (discordData.containsKey("bot-id")) config.setDiscordBotId(String.valueOf(discordData.get("bot-id")));
                }
                
                if (data.containsKey("settings")) {
                    java.util.Map<String, Object> settingsData = (java.util.Map<String, Object>) data.get("settings");
                    if (settingsData.containsKey("prefix")) config.setPrefix(String.valueOf(settingsData.get("prefix")));
                    if (settingsData.containsKey("party-limit-default")) config.setPartyLimitDefault(Integer.parseInt(String.valueOf(settingsData.get("party-limit-default"))));
                    if (settingsData.containsKey("friend-limit-default")) config.setFriendLimitDefault(Integer.parseInt(String.valueOf(settingsData.get("friend-limit-default"))));
                }
                return config;
            } catch (Exception e) {
                logger.error("Could not read config.yml", e);
            }
        }
        return new ProxyConfig();
    }
    
    public void reloadConfig() {
        this.config = loadConfig();
        logger.info("Configuration reloaded");
    }
    
    public ProxyConfig getConfig() {
        return config;
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

    public fr.corehost.api.database.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public fr.corehost.api.profile.ProfileManager getProfileManager() {
        return profileManager;
    }

    public fr.corehost.proxy.auth.AuthManager getAuthManager() {
        return authManager;
    }

    public fr.corehost.proxy.discord.DiscordManager getDiscordManager() {
        return discordManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    private void registerCloudNet() {
        eu.cloudnetservice.driver.inject.InjectionLayer.ext().instance(eu.cloudnetservice.driver.event.EventManager.class).registerListener(new fr.corehost.proxy.cloudnet.CloudNetListener(this, server));
        logger.info("CloudNetListener successfully registered.");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyConfig getProxyConfig() {
        return loadConfig();
    }

    @Subscribe
    public void onProxyShutdown(com.velocitypowered.api.event.proxy.ProxyShutdownEvent event) {
        if (discordManager != null) {
            discordManager.stop();
        }
        if (redisManager != null) {
            redisManager.close();
        }
    }
}
