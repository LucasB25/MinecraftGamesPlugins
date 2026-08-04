package fr.corehost.staffmod.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.corehost.api.redis.RedisManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ReportManager {
    
    private final RedisManager redisManager;
    private final Gson gson = new Gson();
    
    public static class CachedMessage {
        private final String senderName;
        private final String content;
        private final String server; // For cross-server tracking
        private final long timestamp;

        public CachedMessage(String senderName, String content, String server, long timestamp) {
            this.senderName = senderName;
            this.content = content;
            this.server = server;
            this.timestamp = timestamp;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getContent() {
            return content;
        }
        
        public String getServer() {
            return server;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }

    // Cache messages for 15 minutes locally to allow clicking the report button
    private final Cache<UUID, CachedMessage> localMessageCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    public ReportManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    // Called by ChatListener for every message sent
    public UUID cacheLocalMessage(String senderName, String content) {
        UUID id = UUID.randomUUID();
        localMessageCache.put(id, new CachedMessage(senderName, content, "unknown", System.currentTimeMillis()));
        return id;
    }

    // Called by ReportMessageCommand to get the message to report
    public CachedMessage getLocalMessage(UUID id) {
        return localMessageCache.getIfPresent(id);
    }
    
    // Called by ReportMessageCommand to finalize the report and make it global
    public void createActiveReport(UUID id, CachedMessage msg, String server) {
        JsonObject json = new JsonObject();
        json.addProperty("senderName", msg.getSenderName());
        json.addProperty("content", msg.getContent());
        json.addProperty("server", server);
        json.addProperty("timestamp", msg.getTimestamp());
        
        redisManager.hset("corehost:reports:active", id.toString(), gson.toJson(json));
    }

    // Called by GUI to get all active reports
    public Map<UUID, CachedMessage> getAllActiveReports() {
        Map<UUID, CachedMessage> map = new HashMap<>();
        Map<String, String> reports = redisManager.hgetAll("corehost:reports:active");
        for (Map.Entry<String, String> entry : reports.entrySet()) {
            JsonObject json = gson.fromJson(entry.getValue(), JsonObject.class);
            map.put(UUID.fromString(entry.getKey()), new CachedMessage(
                json.get("senderName").getAsString(),
                json.get("content").getAsString(),
                json.has("server") ? json.get("server").getAsString() : "unknown",
                json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis()
            ));
        }
        return map;
    }
    
    public void deleteReport(UUID id) {
        redisManager.hdel("corehost:reports:active", id.toString());
    }
}
