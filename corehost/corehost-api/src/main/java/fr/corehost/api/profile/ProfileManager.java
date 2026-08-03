package fr.corehost.api.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.corehost.api.database.DatabaseManager;
import fr.corehost.api.redis.RedisManager;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ProfileManager {

    private final DatabaseManager databaseManager;
    private final RedisManager redisManager;
    private final Logger logger;
    
    private final Cache<UUID, PlayerProfile> profileCache;
    private final Gson gson;

    public ProfileManager(DatabaseManager databaseManager, RedisManager redisManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.redisManager = redisManager;
        this.logger = logger;
        this.gson = new Gson();
        
        this.profileCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();
                
        if (redisManager != null && redisManager.isConnected()) {
            redisManager.subscribe(new ProfilePubSubListener(this, logger), "corehost:profile:update");
        }
    }
    
    /**
     * Gets a profile from cache or loads it synchronously from DB.
     */
    public PlayerProfile getProfile(UUID uuid) {
        if (uuid == null) return null;
        
        PlayerProfile profile = profileCache.getIfPresent(uuid);
        if (profile == null) {
            if (redisManager != null && redisManager.isConnected()) {
                try (Jedis jedis = redisManager.getPool().getResource()) {
                    String json = jedis.get("corehost:profile:data:" + uuid.toString());
                    if (json != null) {
                        profile = gson.fromJson(json, PlayerProfile.class);
                    }
                } catch (Exception e) {
                    logger.severe("Failed to load profile from redis for " + uuid + ": " + e.getMessage());
                }
            }

            if (profile == null) {
                profile = loadProfileFromStorage(uuid);
                if (profile != null) {
                    saveProfileToRedis(profile);
                }
            }

            if (profile != null) {
                profileCache.put(uuid, profile);
            }
        }
        return profile;
    }
    
    /**
     * Loads a profile completely from MySQL & Redis.
     */
    private PlayerProfile loadProfileFromStorage(UUID uuid) {
        if (databaseManager == null) return null;
        
        PlayerProfile profile = null;
        
        // 1. Load base data from MySQL
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name, last_seen, requests_blocked, coins FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    profile = new PlayerProfile(uuid, rs.getString("name"));
                    profile.setLastSeen(rs.getLong("last_seen"));
                    profile.setRequestsBlocked(rs.getBoolean("requests_blocked"));
                    profile.setCoins(rs.getInt("coins"));
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load profile for " + uuid + ": " + e.getMessage());
            return null;
        }
        
        // Player not found in DB
        if (profile == null) {
            return null;
        }
        
        // 2. Load friends
        Set<String> friends = new HashSet<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT player2_uuid FROM friends WHERE player1_uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friends.add(rs.getString("player2_uuid"));
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load friends for " + uuid + ": " + e.getMessage());
        }
        profile.setFriends(friends);
        
        // 3. Load Redis settings (notifications)
        if (redisManager != null && redisManager.isConnected()) {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                String val = jedis.get("corehost:settings:notifications:" + uuid.toString());
                profile.setNotificationsEnabled(val == null || !val.equals("false"));
            } catch (Exception e) {
                 logger.severe("Failed to load redis settings for " + uuid + ": " + e.getMessage());
            }
        }
        
        return profile;
    }
    
    /**
     * Updates the cache with a new profile instance.
     */
    public void updateCache(PlayerProfile profile) {
        if (profile != null) {
            profileCache.put(profile.getUuid(), profile);
        }
    }

    /**
     * Invalidates a profile in the local cache. Next getProfile() will trigger a DB fetch.
     */
    public void invalidateProfile(UUID uuid) {
        profileCache.invalidate(uuid);
    }
    
    /**
     * Publishes an update message to Redis so all other servers invalidate their cache for this player.
     */
    public void publishProfileUpdate(UUID uuid) {
        if (redisManager != null && redisManager.isConnected()) {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                jedis.publish("corehost:profile:update", uuid.toString());
            } catch (Exception e) {
                logger.severe("Failed to publish profile update for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Invalidates Redis cache and publishes an update message.
     */
    public void syncAndInvalidateCache(UUID uuid) {
        if (redisManager != null && redisManager.isConnected()) {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                jedis.del("corehost:profile:data:" + uuid.toString());
            } catch (Exception e) {
                logger.severe("Failed to invalidate redis profile data for " + uuid + ": " + e.getMessage());
            }
        }
        publishProfileUpdate(uuid);
    }

    /**
     * Adds coins to a player and syncs it.
     */
    public void addCoins(UUID uuid, int amount) {
        if (databaseManager == null) return;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE players SET coins = coins + ? WHERE uuid = ?")) {
            stmt.setInt(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
            
            syncAndInvalidateCache(uuid);
        } catch (SQLException e) {
            logger.severe("Failed to add coins to " + uuid + ": " + e.getMessage());
        }
    }

    public void saveProfileToRedis(PlayerProfile profile) {
        if (redisManager != null && redisManager.isConnected()) {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                // Expire in 1 hour (3600s) if not updated
                jedis.setex("corehost:profile:data:" + profile.getUuid().toString(), 3600, gson.toJson(profile));
            } catch (Exception e) {
                logger.severe("Failed to save profile to redis for " + profile.getUuid() + ": " + e.getMessage());
            }
        }
    }
    
    public void saveProfileToDatabase(PlayerProfile profile) {
        if (databaseManager == null) return;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE players SET name = ?, last_seen = ?, requests_blocked = ?, coins = ? WHERE uuid = ?")) {
            stmt.setString(1, profile.getName());
            stmt.setLong(2, profile.getLastSeen());
            stmt.setBoolean(3, profile.isRequestsBlocked());
            stmt.setInt(4, profile.getCoins());
            stmt.setString(5, profile.getUuid().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save profile to database for " + profile.getUuid() + ": " + e.getMessage());
        }
    }
}
