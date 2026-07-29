package fr.corehost.api.party;

import fr.corehost.api.redis.RedisManager;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyManager {

    private final RedisManager redisManager;
    private static final int DEFAULT_LIMIT = 4;

    public PartyManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /**
     * Crée un nouveau groupe avec le leader spécifié.
     */
    public void createParty(UUID leader) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.set("corehost:party:leader_of:" + leader.toString(), leader.toString());
            jedis.sadd("corehost:party:members:" + leader.toString(), leader.toString());
        }
    }

    /**
     * Dissout le groupe du leader spécifié.
     */
    public void disbandParty(UUID leader) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            Set<String> members = jedis.smembers("corehost:party:members:" + leader.toString());
            for (String member : members) {
                jedis.del("corehost:party:leader_of:" + member);
            }
            jedis.del("corehost:party:members:" + leader.toString());
        }
    }

    /**
     * Récupère le leader du groupe auquel appartient le joueur.
     */
    public UUID getPartyLeader(UUID player) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String leaderStr = jedis.get("corehost:party:leader_of:" + player.toString());
            return leaderStr != null ? UUID.fromString(leaderStr) : null;
        }
    }

    /**
     * Récupère les membres du groupe d'un leader.
     */
    public Set<UUID> getPartyMembers(UUID leader) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            Set<String> members = jedis.smembers("corehost:party:members:" + leader.toString());
            return members.stream().map(UUID::fromString).collect(Collectors.toSet());
        }
    }

    /**
     * Ajoute un membre au groupe du leader.
     */
    public void addMember(UUID leader, UUID member) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.sadd("corehost:party:members:" + leader.toString(), member.toString());
            jedis.set("corehost:party:leader_of:" + member.toString(), leader.toString());
        }
    }

    /**
     * Retire un membre du groupe.
     */
    public void removeMember(UUID member) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String leaderStr = jedis.get("corehost:party:leader_of:" + member.toString());
            if (leaderStr != null) {
                jedis.srem("corehost:party:members:" + leaderStr, member.toString());
                jedis.del("corehost:party:leader_of:" + member.toString());
            }
        }
    }

    /**
     * Envoie une invitation de groupe.
     */
    public void sendInvite(UUID sender, UUID target) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            // L'invitation expire après 60 secondes
            jedis.setex("corehost:party:invites:" + target.toString() + ":" + sender.toString(), 60, "true");
        }
    }

    /**
     * Vérifie si le joueur a une invitation du sender.
     */
    public boolean hasInvite(UUID target, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return jedis.exists("corehost:party:invites:" + target.toString() + ":" + sender.toString());
        }
    }

    /**
     * Retire l'invitation.
     */
    public void removeInvite(UUID target, UUID sender) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.del("corehost:party:invites:" + target.toString() + ":" + sender.toString());
        }
    }

    /**
     * Récupère la limite de membres du groupe.
     */
    public int getPartyLimit(UUID leader) {
        return DEFAULT_LIMIT;
    }
}
