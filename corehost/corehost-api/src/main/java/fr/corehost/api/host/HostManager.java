package fr.corehost.api.host;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.corehost.api.redis.RedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HostManager {

    private static final String HOST_PREFIX = "corehost:hosts:";
    private final RedisManager redisManager;
    private final Gson gson;

    public HostManager(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.gson = new GsonBuilder().create();
    }

    public CompletableFuture<Void> saveHost(HostData hostData) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                String json = gson.toJson(hostData);
                jedis.set(HOST_PREFIX + hostData.getHostId().toString(), json);
                jedis.sadd("corehost:hosts:active_ids", hostData.getHostId().toString());
                jedis.expire(HOST_PREFIX + hostData.getHostId().toString(), 86400); 
            } catch (JedisException e) {
                // Log silently or ignore to prevent crashing the server
            }
        });
    }

    public CompletableFuture<HostData> getHost(UUID hostId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                String json = jedis.get(HOST_PREFIX + hostId.toString());
                if (json != null) {
                    return gson.fromJson(json, HostData.class);
                }
            } catch (JedisException e) {
                // Ignored if Redis is down
            }
            return null;
        });
    }

    public CompletableFuture<List<HostData>> getAllHosts() {
        return CompletableFuture.supplyAsync(() -> {
            List<HostData> hosts = new ArrayList<>();
            try (Jedis jedis = redisManager.getPool().getResource()) {
                Set<String> activeIds = jedis.smembers("corehost:hosts:active_ids");
                if (activeIds != null && !activeIds.isEmpty()) {
                    String[] keys = activeIds.stream().map(id -> HOST_PREFIX + id).toArray(String[]::new);
                    List<String> values = jedis.mget(keys);
                    for (String json : values) {
                        if (json != null) {
                            hosts.add(gson.fromJson(json, HostData.class));
                        }
                    }
                }
            } catch (JedisException e) {
                // Ignored if Redis is down
            }
            return hosts;
        });
    }

    public CompletableFuture<Void> deleteHost(UUID hostId) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = redisManager.getPool().getResource()) {
                jedis.del(HOST_PREFIX + hostId.toString());
                jedis.srem("corehost:hosts:active_ids", hostId.toString());
            } catch (JedisException e) {
                // Ignored if Redis is down
            }
        });
    }

    public CompletableFuture<Void> updateHostStatus(UUID hostId, HostStatus status) {
        return getHost(hostId).thenCompose(hostData -> {
            if (hostData != null) {
                hostData.setStatus(status);
                return saveHost(hostData);
            }
            return CompletableFuture.completedFuture(null);
        });
    }
}
