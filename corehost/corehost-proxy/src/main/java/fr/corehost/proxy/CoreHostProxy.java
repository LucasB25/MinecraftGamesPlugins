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
import fr.corehost.proxy.config.ProxyConfig;
import fr.corehost.proxy.commands.HubCommand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

    @Inject
    public CoreHostProxy(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("CoreHostProxy is starting...");
        
        ProxyConfig config = loadConfig();

        try {
            this.redisManager = new RedisManager(config.getRedisHost(), config.getRedisPort(), config.getRedisPassword());
            this.hostManager = new HostManager(this.redisManager);
            logger.info("Connected to Redis successfully.");
        } catch (Exception e) {
            logger.error("Could not connect to Redis", e);
        }

        // Commands will be handled by the backend Spigot servers
        // so that /spawn in the lobby can teleport the player to the lobby spawn.
    }

    private ProxyConfig loadConfig() {
        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                ProxyConfig defaultConfig = new ProxyConfig();
                gson.toJson(defaultConfig, writer);
                return defaultConfig;
            } catch (IOException e) {
                logger.error("Could not create default config.json", e);
            }
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                return gson.fromJson(reader, ProxyConfig.class);
            } catch (IOException e) {
                logger.error("Could not read config.json", e);
            }
        }
        return new ProxyConfig();
    }
    
    public RedisManager getRedisManager() {
        return redisManager;
    }

    public HostManager getHostManager() {
        return hostManager;
    }
}
