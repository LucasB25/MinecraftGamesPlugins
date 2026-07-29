package fr.corehost.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import fr.corehost.api.redis.RedisManager;

@Plugin(
    id = "corehost-proxy",
    name = "CoreHostProxy",
    version = "1.0-SNAPSHOT",
    authors = {"CoreHost"}
)
public class CoreHostProxy {

    private final ProxyServer server;
    private final Logger logger;
    private RedisManager redisManager;

    @Inject
    public CoreHostProxy(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("CoreHostProxy is starting...");
        
        // Example initialization of Redis
        try {
            this.redisManager = new RedisManager("localhost", 6379, "");
            logger.info("Connected to Redis successfully.");
        } catch (Exception e) {
            logger.error("Could not connect to Redis", e);
        }
    }
    
    public RedisManager getRedisManager() {
        return redisManager;
    }
}
