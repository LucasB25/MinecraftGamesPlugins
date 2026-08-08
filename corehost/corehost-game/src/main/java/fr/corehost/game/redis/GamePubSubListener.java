package fr.corehost.game.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.corehost.api.redis.RedisManager;
import fr.corehost.game.CoreHostGame;
import fr.corehost.game.slime.SlimeManager;
import redis.clients.jedis.JedisPubSub;


public class GamePubSubListener extends JedisPubSub {

    private final CoreHostGame plugin;
    private final SlimeManager slimeManager;
    private final RedisManager redisManager;
    private final String serverName;
    private final Gson gson = new Gson();

    public GamePubSubListener(CoreHostGame plugin, SlimeManager slimeManager, RedisManager redisManager, String serverName) {
        this.plugin = plugin;
        this.slimeManager = slimeManager;
        this.redisManager = redisManager;
        this.serverName = serverName;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals("corehost:game:" + serverName)) return;

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String action = json.get("action").getAsString();

            if ("create_slime_instance".equals(action)) {
                String hostId = json.get("hostId").getAsString();
                String gameType = json.get("gameType").getAsString();
                String defaultTemplate = json.has("templateName") ? json.get("templateName").getAsString() : gameType;
                
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    fr.corehost.game.events.PreSlimeInstanceCreateEvent event = new fr.corehost.game.events.PreSlimeInstanceCreateEvent(hostId, gameType);
                    event.setTemplateName(defaultTemplate);
                    org.bukkit.Bukkit.getPluginManager().callEvent(event);
                    
                    String templateName = event.getTemplateName();
                    plugin.getLogger().info("Received request to create slime instance for host " + hostId + " (game: " + gameType + ", template: " + templateName + ")");
                    
                    // Use the templateName (e.g. yinyang), and hostId as the new world name
                    slimeManager.loadWorld(templateName, hostId);
                });
                
                // After world is loaded (this is async inside SlimeManager)
                // SlimeManager will publish the HOST_READY event once the world is loaded.
            } else if ("PLAYER_JOIN_HOST".equals(action)) {
                String hostId = json.get("hostId").getAsString();
                String playerUuidStr = json.get("playerUuid").getAsString();
                try {
                    java.util.UUID playerUuid = java.util.UUID.fromString(playerUuidStr);
                    plugin.getPendingJoins().put(playerUuid, hostId);
                    plugin.getLogger().info("Registered pending join for player " + playerUuidStr + " to host " + hostId);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Invalid UUID in PLAYER_JOIN_HOST: " + playerUuidStr);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing pubsub message: " + e.getMessage());
        }
    }
}
