package fr.corehost.api.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.corehost.api.database.DatabaseManager;
import fr.corehost.api.redis.RedisManager;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    
    public PlayerProfile getCachedProfile(UUID uuid) {
        if (uuid == null) return null;
        return profileCache.getIfPresent(uuid);
    }
    
    /**
     * Gets a profile asynchronously. It checks the cache first, then Redis, then MySQL.
     */
    public CompletableFuture<PlayerProfile> getProfile(UUID uuid) {
        if (uuid == null) return CompletableFuture.completedFuture(null);
        
        PlayerProfile cached = profileCache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            PlayerProfile profile = null;
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
            return profile;
        }).thenCompose(profile -> {
            if (profile != null) {
                profileCache.put(uuid, profile);
                return CompletableFuture.completedFuture(profile);
            }
            return loadProfileFromStorage(uuid).thenApply(loaded -> {
                if (loaded != null) {
                    saveProfileToRedis(loaded);
                    profileCache.put(uuid, loaded);
                }
                return loaded;
            });
        });
    }
    
    /**
     * Loads a profile completely from MySQL & Redis via DAOs.
     */
    private CompletableFuture<PlayerProfile> loadProfileFromStorage(UUID uuid) {
        if (databaseManager == null) return CompletableFuture.completedFuture(null);
        
        return databaseManager.getProfileDAO().loadProfile(uuid).thenCompose(profile -> {
            if (profile == null) return CompletableFuture.completedFuture(null);
            
            // Load friends and stats in parallel
            CompletableFuture<Void> friendsFuture = databaseManager.getFriendDAO().loadFriends(uuid)
                .thenAccept(profile::setFriends);
                
            CompletableFuture<Void> statsFuture = databaseManager.getStatsDAO().loadStats(uuid)
                .thenAccept(stats -> {
                    stats.forEach((game, gameStats) -> {
                        gameStats.forEach((stat, val) -> profile.setStat(game, stat, val));
                    });
                });
                
            return CompletableFuture.allOf(friendsFuture, statsFuture).thenApply(v -> {
                // Load Redis settings (notifications)
                if (redisManager != null && redisManager.isConnected()) {
                    try (Jedis jedis = redisManager.getPool().getResource()) {
                        String val = jedis.get("corehost:settings:notifications:" + uuid.toString());
                        profile.setNotificationsEnabled(val == null || !val.equals("false"));
                    } catch (Exception e) {
                         logger.severe("Failed to load redis settings for " + uuid + ": " + e.getMessage());
                    }
                }
                return profile;
            });
        });
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
     * Forces an asynchronous load of the profile from storage and updates the cache.
     */
    public CompletableFuture<Void> forceUpdateProfile(UUID uuid) {
        return loadProfileFromStorage(uuid).thenAccept(profile -> {
            if (profile != null) {
                updateCache(profile);
                saveProfileToRedis(profile);
            }
        });
    }
    
    /**
     * Publishes an update message to Redis so all other servers invalidate their cache for this player.
     */
    public void publishProfileUpdate(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            if (redisManager != null && redisManager.isConnected()) {
                try (Jedis jedis = redisManager.getPool().getResource()) {
                    jedis.publish("corehost:profile:update", uuid.toString());
                } catch (Exception e) {
                    logger.severe("Failed to publish profile update for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    /**
     * Invalidates Redis cache and publishes an update message.
     */
    public void syncAndInvalidateCache(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            if (redisManager != null && redisManager.isConnected()) {
                try (Jedis jedis = redisManager.getPool().getResource()) {
                    jedis.del("corehost:profile:data:" + uuid.toString());
                } catch (Exception e) {
                    logger.severe("Failed to invalidate redis profile data for " + uuid + ": " + e.getMessage());
                }
            }
            publishProfileUpdate(uuid);
        });
    }

    /**
     * Adds coins to a player and syncs it.
     */
    public void addCoins(UUID uuid, int amount) {
        if (databaseManager == null) return;
        
        databaseManager.getProfileDAO().addCoins(uuid, amount).thenRun(() -> {
            syncAndInvalidateCache(uuid);
        });
    }

    public void saveProfileToRedis(PlayerProfile profile) {
        CompletableFuture.runAsync(() -> {
            if (redisManager != null && redisManager.isConnected()) {
                try (Jedis jedis = redisManager.getPool().getResource()) {
                    jedis.set("corehost:profile:data:" + profile.getUuid().toString(), gson.toJson(profile), redis.clients.jedis.params.SetParams.setParams().ex(3600));
                } catch (Exception e) {
                    logger.severe("Failed to save profile to redis for " + profile.getUuid() + ": " + e.getMessage());
                }
            }
        });
    }
    
    public void saveProfileToDatabase(PlayerProfile profile) {
        if (databaseManager == null) return;
        
        databaseManager.getProfileDAO().saveProfile(profile).thenRun(() -> {
            // Also save stats
            profile.getStats().forEach((game, stats) -> {
                stats.forEach((statKey, statValue) -> {
                    databaseManager.getStatsDAO().saveStat(profile.getUuid(), game, statKey, statValue);
                });
            });
        });
    }

    public void saveAllProfilesSync() {
        if (databaseManager == null) return;
        
        for (PlayerProfile profile : profileCache.asMap().values()) {
            try {
                databaseManager.getProfileDAO().saveProfile(profile).join();
                profile.getStats().forEach((game, stats) -> {
                    stats.forEach((statKey, statValue) -> {
                        databaseManager.getStatsDAO().saveStat(profile.getUuid(), game, statKey, statValue).join();
                    });
                });
                logger.info("Saved profile " + profile.getUuid() + " to database on shutdown.");
            } catch (Exception e) {
                logger.severe("Failed to save profile " + profile.getUuid() + " on shutdown: " + e.getMessage());
            }
        }
    }
}
