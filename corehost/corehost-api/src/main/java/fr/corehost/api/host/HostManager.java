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

public class HostManager {

    private static final String HOST_PREFIX = "corehost:hosts:";
    private final RedisManager redisManager;
    private final Gson gson;

    public HostManager(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.gson = new GsonBuilder().create();
    }

    public void saveHost(HostData hostData) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String json = gson.toJson(hostData);
            jedis.set(HOST_PREFIX + hostData.getHostId().toString(), json);
            jedis.sadd("corehost:hosts:active_ids", hostData.getHostId().toString());
            // Expire after 24 hours just in case of ghost servers
            jedis.expire(HOST_PREFIX + hostData.getHostId().toString(), 86400); 
        } catch (JedisException e) {
            // Log silently or ignore to prevent crashing the server
        }
    }

    public HostData getHost(UUID hostId) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String json = jedis.get(HOST_PREFIX + hostId.toString());
            if (json != null) {
                return gson.fromJson(json, HostData.class);
            }
        } catch (JedisException e) {
            // Ignored if Redis is down
        }
        return null;
    }

    public List<HostData> getAllHosts() {
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
    }

    public void deleteHost(UUID hostId) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.del(HOST_PREFIX + hostId.toString());
            jedis.srem("corehost:hosts:active_ids", hostId.toString());
        } catch (JedisException e) {
            // Ignored if Redis is down
        }
    }

    public void updateHostStatus(UUID hostId, HostStatus status) {
        HostData hostData = getHost(hostId);
        if (hostData != null) {
            hostData.setStatus(status);
            saveHost(hostData);
        }
    }
}
