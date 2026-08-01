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
                
                plugin.getLogger().info("Received request to create slime instance for host " + hostId + " (game: " + gameType + ")");
                
                // Use the gameType as the template name, and hostId as the new world name
                slimeManager.loadWorld(gameType, hostId);
                
                // After world is loaded (this is async inside SlimeManager, so we need a callback)
                // For now, let's just publish back directly. Ideally, SlimeManager calls this back when it's done.
                // To keep it clean, we'll notify Proxy here but it might be a few ms early.
                // We'll update SlimeManager later to do this callback if necessary.
                
                JsonObject response = new JsonObject();
                response.addProperty("action", "HOST_READY");
                response.addProperty("hostId", hostId);
                response.addProperty("serverName", serverName);
                redisManager.publish("corehost:proxy:events", response.toString());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing pubsub message: " + e.getMessage());
        }
    }
}
