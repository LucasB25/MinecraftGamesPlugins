package fr.corehost.api.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import redis.clients.jedis.JedisPubSub;

@SuppressWarnings("deprecation")
public class RedisManager {
    
    private final JedisPool jedisPool;

    public RedisManager(String host, int port, String password) {
        redis.clients.jedis.JedisPoolConfig poolConfig = new redis.clients.jedis.JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        if (password != null && !password.isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        } else {
            this.jedisPool = new JedisPool(poolConfig, host, port, 2000);
        }
    }

    public JedisPool getPool() {
        return jedisPool;
    }

    public boolean isConnected() {
        if (jedisPool == null || jedisPool.isClosed()) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public String get(String key) {
        if (jedisPool == null || jedisPool.isClosed()) return null;
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public void set(String key, String value) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception ignored) {}
    }

    public void setEx(String key, String value, int seconds) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value, redis.clients.jedis.params.SetParams.setParams().ex(seconds));
        } catch (Exception ignored) {}
    }

    public void del(String key) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception ignored) {}
    }

    public void publish(String channel, String message) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception ignored) {}
    }
    
    public void hset(String key, String field, String value) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(key, field, value);
        } catch (Exception ignored) {}
    }
    
    public void hdel(String key, String field) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(key, field);
        } catch (Exception ignored) {}
    }
    
    public java.util.Map<String, String> hgetAll(String key) {
        if (jedisPool == null || jedisPool.isClosed()) return java.util.Collections.emptyMap();
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(key);
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }
    
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
        new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(jedisPubSub, channels);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Redis-PubSub-Thread").start();
    }
}
