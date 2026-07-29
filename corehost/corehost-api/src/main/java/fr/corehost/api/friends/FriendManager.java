package fr.corehost.api.friends;

import fr.corehost.api.redis.RedisManager;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.UUID;

public class FriendManager {

    private final RedisManager redisManager;

    public FriendManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /**
     * Cache le joueur avec son pseudo en minuscules (pour la recherche) et son UUID.
     */
    public void cachePlayer(String name, UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String nameLower = name.toLowerCase();
            jedis.set("corehost:name_to_uuid:" + nameLower, uuid.toString());
            jedis.set("corehost:uuid_to_name:" + uuid.toString(), name);
        }
    }

    public UUID getUuidByName(String name) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String uuidStr = jedis.get("corehost:name_to_uuid:" + name.toLowerCase());
            return uuidStr != null ? UUID.fromString(uuidStr) : null;
        }
    }

    public String getNameByUuid(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.get("corehost:uuid_to_name:" + uuid.toString());
        }
    }

    public Set<String> getFriends(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.smembers("corehost:friends:" + uuid.toString());
        }
    }

    public Set<String> getFriendRequests(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.smembers("corehost:friend_requests:" + uuid.toString());
        }
    }

    public boolean areFriends(UUID player1, UUID player2) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.sismember("corehost:friends:" + player1.toString(), player2.toString());
        }
    }

    public boolean hasFriendRequest(UUID target, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.sismember("corehost:friend_requests:" + target.toString(), sender.toString());
        }
    }

    public void sendFriendRequest(UUID sender, UUID target) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.sadd("corehost:friend_requests:" + target.toString(), sender.toString());
        }
    }

    public void acceptFriendRequest(UUID receiver, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            // Remove request
            jedis.srem("corehost:friend_requests:" + receiver.toString(), sender.toString());
            // Add to both friends lists
            jedis.sadd("corehost:friends:" + receiver.toString(), sender.toString());
            jedis.sadd("corehost:friends:" + sender.toString(), receiver.toString());
        }
    }

    public void denyFriendRequest(UUID receiver, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.srem("corehost:friend_requests:" + receiver.toString(), sender.toString());
        }
    }

    public void removeFriend(UUID player1, UUID player2) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.srem("corehost:friends:" + player1.toString(), player2.toString());
            jedis.srem("corehost:friends:" + player2.toString(), player1.toString());
        }
    }

    public void setFriendRequestsBlocked(UUID uuid, boolean blocked) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:settings:requests_blocked:" + uuid.toString();
            if (blocked) {
                jedis.set(key, "true");
            } else {
                jedis.del(key);
            }
        }
    }

    public boolean areFriendRequestsBlocked(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String val = jedis.get("corehost:settings:requests_blocked:" + uuid.toString());
            return val != null && val.equals("true");
        }
    }

    public void updateLastSeen(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.set("corehost:lastseen:" + uuid.toString(), String.valueOf(System.currentTimeMillis()));
        }
    }

    public long getLastSeen(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String val = jedis.get("corehost:lastseen:" + uuid.toString());
            if (val != null) {
                try {
                    return Long.parseLong(val);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return 0;
        }
    }

    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:settings:notifications_disabled:" + uuid.toString();
            if (!enabled) {
                jedis.set(key, "true");
            } else {
                jedis.del(key);
            }
        }
    }

    public boolean areNotificationsEnabled(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String val = jedis.get("corehost:settings:notifications_disabled:" + uuid.toString());
            return val == null || !val.equals("true"); // Default to true if key does not exist
        }
    }

    public void setOnline(UUID uuid, boolean online) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:online:" + uuid.toString();
            if (online) {
                jedis.set(key, "true");
            } else {
                jedis.del(key);
            }
        }
    }

    public boolean isOnline(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.exists("corehost:online:" + uuid.toString());
        }
    }
}
