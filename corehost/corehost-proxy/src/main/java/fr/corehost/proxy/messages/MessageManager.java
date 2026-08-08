package fr.corehost.proxy.messages;

import fr.corehost.proxy.CoreHostProxy;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MessageManager {

    private final CoreHostProxy plugin;
    // Store last conversations for /r (memory only, reset on proxy restart)
    private final Map<UUID, UUID> lastConversations = new HashMap<>();

    public MessageManager(CoreHostProxy plugin) {
        this.plugin = plugin;
    }

    public void setLastMessaged(UUID sender, UUID target) {
        lastConversations.put(sender, target);
        lastConversations.put(target, sender);
    }

    public UUID getLastMessaged(UUID player) {
        return lastConversations.get(player);
    }

    public void removeLastMessaged(UUID player) {
        lastConversations.remove(player);
    }

    // --- Redis Integration ---

    public boolean isMessagesBlocked(UUID uuid) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return false;
        
        // Mod Mode check
        String modMode = plugin.getRedisManager().get("corehost:modmode:" + uuid.toString());
        if ("true".equals(modMode)) {
            String modMsg = plugin.getRedisManager().get("corehost:modmsg:" + uuid.toString());
            if (!"true".equals(modMsg)) {
                return true;
            }
        }
        
        String val = plugin.getRedisManager().get("corehost:messages:blocked:" + uuid.toString());
        return "true".equals(val);
    }

    public void setMessagesBlocked(UUID uuid, boolean blocked) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return;
        if (blocked) {
            plugin.getRedisManager().set("corehost:messages:blocked:" + uuid.toString(), "true");
        } else {
            plugin.getRedisManager().del("corehost:messages:blocked:" + uuid.toString());
        }
    }

    public boolean isIgnoring(UUID player, UUID target) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return false;
        try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
            return jedis.sismember("corehost:messages:ignored:" + player.toString(), target.toString());
        } catch (Exception e) {
            plugin.getLogger().error("Error checking ignore status for " + player, e);
            return false;
        }
    }

    public void addIgnore(UUID player, UUID target) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return;
        try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
            jedis.sadd("corehost:messages:ignored:" + player.toString(), target.toString());
        } catch (Exception e) {
            plugin.getLogger().error("Error adding ignore for " + player, e);
        }
    }

    public void removeIgnore(UUID player, UUID target) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return;
        try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
            jedis.srem("corehost:messages:ignored:" + player.toString(), target.toString());
        } catch (Exception e) {
            plugin.getLogger().error("Error removing ignore for " + player, e);
        }
    }
    
    public Set<UUID> getIgnoredPlayers(UUID player) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return new HashSet<>();
        try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
            Set<String> members = jedis.smembers("corehost:messages:ignored:" + player.toString());
            Set<UUID> uuids = new HashSet<>();
            for (String m : members) {
                try {
                    uuids.add(UUID.fromString(m));
                } catch (IllegalArgumentException ignored) {}
            }
            return uuids;
        } catch (Exception e) {
            plugin.getLogger().error("Error getting ignored players for " + player, e);
            return new HashSet<>();
        }
    }
}
