package fr.corehost.api.profile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProfile {

    private final UUID uuid;
    private String name;
    
    private final Set<String> friends;
    
    private boolean requestsBlocked;
    private boolean notificationsEnabled;
    private long lastSeen;
    private int coins;
    private final Map<String, Map<String, Integer>> stats;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.friends = new HashSet<>();
        this.requestsBlocked = false;
        this.notificationsEnabled = true;
        this.lastSeen = 0;
        this.coins = 0;
        this.stats = new ConcurrentHashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getFriends() {
        return friends;
    }

    public void setFriends(Set<String> friends) {
        this.friends.clear();
        if (friends != null) {
            this.friends.addAll(friends);
        }
    }
    
    public void addFriend(String friendUuid) {
        this.friends.add(friendUuid);
    }
    
    public void removeFriend(String friendUuid) {
        this.friends.remove(friendUuid);
    }
    
    public boolean hasFriend(String friendUuid) {
        return this.friends.contains(friendUuid);
    }

    public boolean isRequestsBlocked() {
        return requestsBlocked;
    }

    public void setRequestsBlocked(boolean requestsBlocked) {
        this.requestsBlocked = requestsBlocked;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public Map<String, Map<String, Integer>> getStats() {
        return stats;
    }

    public int getStat(String game, String stat) {
        return stats.getOrDefault(game, new ConcurrentHashMap<>()).getOrDefault(stat, 0);
    }

    public void setStat(String game, String stat, int value) {
        stats.computeIfAbsent(game, k -> new ConcurrentHashMap<>()).put(stat, value);
    }

    public void addStat(String game, String stat, int value) {
        setStat(game, stat, getStat(game, stat) + value);
    }
}
