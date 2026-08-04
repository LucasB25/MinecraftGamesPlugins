package fr.corehost.api.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

public class RedisManager {
    
    private final JedisPool jedisPool;

    public RedisManager(String host, int port, String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
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
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (JedisException e) {
            return false;
        }
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (JedisException e) {
            return null;
        }
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (JedisException ignored) {}
    }

    public void setEx(String key, String value, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        } catch (JedisException ignored) {}
    }

    public void del(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (JedisException ignored) {}
    }

    public void publish(String channel, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (JedisException ignored) {}
    }
    
    public void hset(String key, String field, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(key, field, value);
        } catch (JedisException ignored) {}
    }
    
    public void hdel(String key, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(key, field);
        } catch (JedisException ignored) {}
    }
    
    public java.util.Map<String, String> hgetAll(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(key);
        } catch (JedisException e) {
            return java.util.Collections.emptyMap();
        }
    }
    
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
        new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(jedisPubSub, channels);
            } catch (JedisException e) {
                e.printStackTrace();
            }
        }, "Redis-PubSub-Thread").start();
    }
}
