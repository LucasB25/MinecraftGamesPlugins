package fr.corehost.api.friends;

import fr.corehost.api.database.DatabaseManager;
import fr.corehost.api.redis.RedisManager;
import fr.corehost.api.profile.ProfileManager;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FriendManager {

    private final RedisManager redisManager;
    private final DatabaseManager databaseManager;
    private final ProfileManager profileManager;

    public FriendManager(RedisManager redisManager, DatabaseManager databaseManager, ProfileManager profileManager) {
        this.redisManager = redisManager;
        this.databaseManager = databaseManager;
        this.profileManager = profileManager;
    }

    // For backwards compatibility when DatabaseManager is not yet fully initialized
    public FriendManager(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.databaseManager = null;
        this.profileManager = null;
    }

    /**
     * Cache le joueur avec son pseudo et son UUID dans SQL.
     */
    public void cachePlayer(String name, UUID uuid) {
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO players (uuid, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setString(3, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UUID getUuidByName(String name) {
        // Not easily cacheable by UUID without a reverse lookup cache, keeping SQL for now
        if (databaseManager == null) return null;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM players WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNameByUuid(UUID uuid) {
        if (profileManager != null) {
            fr.corehost.api.profile.PlayerProfile profile = profileManager.getProfile(uuid);
            if (profile != null) return profile.getName();
        }
        if (databaseManager == null) return null;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Set<String> getFriends(UUID uuid) {
        if (profileManager != null) {
            fr.corehost.api.profile.PlayerProfile profile = profileManager.getProfile(uuid);
            if (profile != null) return profile.getFriends();
        }
        Set<String> friends = new HashSet<>();
        if (databaseManager == null) return friends;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT player2_uuid FROM friends WHERE player1_uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friends.add(rs.getString("player2_uuid"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    // Friend requests are still in Redis because they expire
    public Set<String> getFriendRequests(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:friend_requests:" + uuid.toString();
            long now = System.currentTimeMillis();
            jedis.zremrangeByScore(key, 0, now);
            return new HashSet<>(jedis.zrange(key, 0, -1));
        }
    }

    public boolean areFriends(UUID player1, UUID player2) {
        if (profileManager != null) {
            fr.corehost.api.profile.PlayerProfile profile = profileManager.getProfile(player1);
            if (profile != null) return profile.hasFriend(player2.toString());
        }
        if (databaseManager == null) return false;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM friends WHERE player1_uuid = ? AND player2_uuid = ?")) {
            stmt.setString(1, player1.toString());
            stmt.setString(2, player2.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasFriendRequest(UUID target, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:friend_requests:" + target.toString();
            long now = System.currentTimeMillis();
            jedis.zremrangeByScore(key, 0, now);
            return jedis.zscore(key, sender.toString()) != null;
        }
    }

    public void sendFriendRequest(UUID sender, UUID target) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:friend_requests:" + target.toString();
            long expireTime = System.currentTimeMillis() + 60000; // 60 seconds
            jedis.zadd(key, expireTime, sender.toString());
        }
    }

    public void acceptFriendRequest(UUID receiver, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.zrem("corehost:friend_requests:" + receiver.toString(), sender.toString());
        }
        
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT IGNORE INTO friends (player1_uuid, player2_uuid) VALUES (?, ?), (?, ?)")) {
            stmt.setString(1, receiver.toString());
            stmt.setString(2, sender.toString());
            stmt.setString(3, sender.toString());
            stmt.setString(4, receiver.toString());
            stmt.executeUpdate();
            
            if (profileManager != null) {
                profileManager.syncAndInvalidateCache(receiver);
                profileManager.syncAndInvalidateCache(sender);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void denyFriendRequest(UUID receiver, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.zrem("corehost:friend_requests:" + receiver.toString(), sender.toString());
        }
    }

    public void removeFriend(UUID player1, UUID player2) {
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM friends WHERE (player1_uuid = ? AND player2_uuid = ?) OR (player1_uuid = ? AND player2_uuid = ?)")) {
            stmt.setString(1, player1.toString());
            stmt.setString(2, player2.toString());
            stmt.setString(3, player2.toString());
            stmt.setString(4, player1.toString());
            stmt.executeUpdate();
            
            if (profileManager != null) {
                profileManager.syncAndInvalidateCache(player1);
                profileManager.syncAndInvalidateCache(player2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setFriendRequestsBlocked(UUID uuid, boolean blocked) {
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE players SET requests_blocked = ? WHERE uuid = ?")) {
            stmt.setBoolean(1, blocked);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
            
            if (profileManager != null) {
                profileManager.syncAndInvalidateCache(uuid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean areFriendRequestsBlocked(UUID uuid) {
        if (profileManager != null) {
            fr.corehost.api.profile.PlayerProfile profile = profileManager.getProfile(uuid);
            if (profile != null) return profile.isRequestsBlocked();
        }
        if (databaseManager == null) return false;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT requests_blocked FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("requests_blocked");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateLastSeen(UUID uuid) {
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE players SET last_seen = ? WHERE uuid = ?")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
            
            if (profileManager != null) {
                profileManager.syncAndInvalidateCache(uuid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getLastSeen(UUID uuid) {
        if (databaseManager == null) return 0;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT last_seen FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("last_seen");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // --- Redis transient data & settings ---

    public void setOnline(UUID uuid, boolean online) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            if (online) {
                jedis.setex("corehost:online:" + uuid.toString(), 86400, "true");
            } else {
                jedis.del("corehost:online:" + uuid.toString());
            }
        }
    }
    
    public boolean isOnline(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.exists("corehost:online:" + uuid.toString());
        }
    }

    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String key = "corehost:settings:notifications:" + uuid.toString();
            if (!enabled) {
                jedis.set(key, "false");
            } else {
                jedis.del(key);
            }
            if (profileManager != null) {
                profileManager.publishProfileUpdate(uuid);
            }
        }
    }

    public boolean areNotificationsEnabled(UUID uuid) {
        if (profileManager != null) {
            fr.corehost.api.profile.PlayerProfile profile = profileManager.getProfile(uuid);
            if (profile != null) return profile.isNotificationsEnabled();
        }
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String val = jedis.get("corehost:settings:notifications:" + uuid.toString());
            return val == null || !val.equals("false"); // Default is true
        }
    }
}
