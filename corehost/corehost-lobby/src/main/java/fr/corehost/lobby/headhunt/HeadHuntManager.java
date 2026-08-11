package fr.corehost.lobby.headhunt;

import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.Constants;
import fr.corehost.api.profile.PlayerProfile;
import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeadHuntManager {

    private final CoreHostLobby plugin;
    private final List<Location> heads = new ArrayList<>();
    private File headsFile;
    private FileConfiguration headsConfig;

    private static final int COINS_PER_HEAD = 2;
    private static final int FINAL_BONUS = 10;

    private final java.util.Map<UUID, Set<String>> cachedFoundHeads = new java.util.concurrent.ConcurrentHashMap<>();

    public HeadHuntManager(CoreHostLobby plugin) {
        this.plugin = plugin;
        loadHeads();
        startParticleTask();
    }

    private void startParticleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Set<String> found = cachedFoundHeads.get(player.getUniqueId());
                if (found == null) continue;
                
                // Only spawn particles if player is in same world as heads (all heads are in lobby world)
                for (Location head : heads) {
                    if (!player.getWorld().equals(head.getWorld())) continue;
                    
                    // distance check to prevent sending packets too far
                    if (player.getLocation().distanceSquared(head) > 2500) continue; // 50 blocks
                    
                    if (!found.contains(serializeLocation(head))) {
                        // Spawn particle slightly above the block
                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, 
                            head.clone().add(0.5, 0.8, 0.5), 
                            2, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
        }, 20L, 20L);
    }

    private void loadHeads() {
        headsFile = new File(plugin.getDataFolder(), "heads.yml");
        if (!headsFile.exists()) {
            plugin.saveResource("heads.yml", false);
        }
        headsConfig = YamlConfiguration.loadConfiguration(headsFile);
        
        List<String> list = headsConfig.getStringList("heads");
        for (String locStr : list) {
            Location loc = deserializeLocation(locStr);
            if (loc != null) {
                heads.add(loc);
            }
        }
        plugin.getLogger().info("Loaded " + heads.size() + " heads for the Head Hunt.");
    }

    private void saveHeads() {
        List<String> list = heads.stream().map(this::serializeLocation).collect(Collectors.toList());
        headsConfig.set("heads", list);
        try {
            headsConfig.save(headsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save heads.yml: " + e.getMessage());
        }
    }

    public boolean addHead(Location location) {
        for (Location loc : heads) {
            if (isSameBlock(loc, location)) {
                return false;
            }
        }
        heads.add(location);
        saveHeads();
        return true;
    }

    public boolean removeHead(Location location) {
        Location toRemove = null;
        for (Location loc : heads) {
            if (isSameBlock(loc, location)) {
                toRemove = loc;
                break;
            }
        }
        if (toRemove != null) {
            heads.remove(toRemove);
            saveHeads();
            return true;
        }
        return false;
    }
    
    public List<Location> getHeads() {
        return heads;
    }

    public int getTotalHeads() {
        return heads.size();
    }

    public boolean isHeadHuntBlock(Location location) {
        for (Location loc : heads) {
            if (isSameBlock(loc, location)) {
                return true;
            }
        }
        return false;
    }

    public void clickHead(Player player, Location location) {
        if (!isHeadHuntBlock(location)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String headId = serializeLocation(location);
            String redisKey = "corehost:headhunt:" + player.getUniqueId().toString();

            try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                boolean alreadyFound = jedis.sismember(redisKey, headId);

                if (alreadyFound) {
                    player.sendMessage(Constants.PREFIX + CC.RED + "Vous avez déjà trouvé cette tête !");
                    return;
                }

                // Give reward
                PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
                if (profile != null) {
                    profile.setCoins(profile.getCoins() + COINS_PER_HEAD);
                }
                plugin.getProfileManager().addCoins(player.getUniqueId(), COINS_PER_HEAD);

                // Add to redis and cache
                jedis.sadd(redisKey, headId);
                Set<String> cache = cachedFoundHeads.get(player.getUniqueId());
                if (cache != null) cache.add(headId);
                
                long foundCount = jedis.scard(redisKey);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                player.sendMessage(Constants.PREFIX + CC.GREEN + "Vous avez trouvé une tête cachée ! " + CC.YELLOW + "+" + COINS_PER_HEAD + " Coins " + CC.GRAY + "(" + foundCount + "/" + getTotalHeads() + ")");

                // Check final bonus
                if (foundCount >= getTotalHeads() && getTotalHeads() > 0) {
                    if (profile != null) {
                        profile.setCoins(profile.getCoins() + FINAL_BONUS);
                    }
                    plugin.getProfileManager().addCoins(player.getUniqueId(), FINAL_BONUS);
                    player.sendMessage(Constants.PREFIX + CC.GREEN + "" + CC.BOLD + "Félicitations ! " + CC.GREEN + "Vous avez trouvé toutes les têtes ! " + CC.YELLOW + "+" + FINAL_BONUS + " Coins Bonus");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }

                // Update scoreboard immediately
                if (plugin.getScoreboardManager() != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getScoreboardManager().updateScoreboard(player));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur redis HeadHunt pour " + player.getName() + " : " + e.getMessage());
            }
        });
    }

    public int getFoundHeads(UUID uuid) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return 0;
        try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
            return Math.toIntExact(jedis.scard("corehost:headhunt:" + uuid.toString()));
        } catch (Exception e) {
            return 0;
        }
    }

    public void resetPlayer(UUID uuid) {
        if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
            try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                jedis.del("corehost:headhunt:" + uuid.toString());
                Set<String> cache = cachedFoundHeads.get(uuid);
                if (cache != null) cache.clear();
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur redis HeadHunt reset pour " + uuid + " : " + e.getMessage());
            }
        }
    }

    public void loadPlayerCache(UUID uuid) {
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                Set<String> found = jedis.smembers("corehost:headhunt:" + uuid.toString());
                cachedFoundHeads.put(uuid, found != null ? found : new java.util.HashSet<>());
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur chargement cache HeadHunt pour " + uuid + " : " + e.getMessage());
            }
        });
    }

    public void unloadPlayerCache(UUID uuid) {
        cachedFoundHeads.remove(uuid);
    }

    private boolean isSameBlock(Location l1, Location l2) {
        if (!l1.getWorld().getName().equals(l2.getWorld().getName())) return false;
        return l1.getBlockX() == l2.getBlockX() &&
               l1.getBlockY() == l2.getBlockY() &&
               l1.getBlockZ() == l2.getBlockZ();
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private Location deserializeLocation(String str) {
        String[] parts = str.split(";");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
