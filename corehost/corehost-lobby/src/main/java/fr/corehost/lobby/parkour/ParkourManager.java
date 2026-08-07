package fr.corehost.lobby.parkour;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import fr.corehost.lobby.CoreHostLobby;
import fr.corehost.lobby.utils.Constants;

import java.util.*;

@SuppressWarnings("deprecation")
public class ParkourManager {

    private final Map<String, ParkourCourse> courses;
    private final Map<UUID, ActiveParkourSession> activeSessions;
    private final Map<UUID, BukkitTask> timeoutTasks;
    
    private final CoreHostLobby plugin;

    public ParkourManager(CoreHostLobby plugin) {
        this.plugin = plugin;
        this.courses = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.timeoutTasks = new HashMap<>();
        
        loadConfigData();
    }
    
    public void loadConfigData() {
        courses.clear();
        
        // Initialize default courses
        ParkourCourse easyCourse = new ParkourCourse(plugin, "easy", "Facile");
        ParkourCourse hardCourse = new ParkourCourse(plugin, "hard", "Difficile");
        
        // Migration from old config format to new format
        if (plugin.getConfig().contains("parkour.start")) {
            // Migrate to easy
            plugin.getConfig().set("parkour.courses.easy.start", plugin.getConfig().get("parkour.start"));
            plugin.getConfig().set("parkour.start", null);
            
            if (plugin.getConfig().contains("parkour.end")) {
                plugin.getConfig().set("parkour.courses.easy.end", plugin.getConfig().get("parkour.end"));
                plugin.getConfig().set("parkour.end", null);
            }
            if (plugin.getConfig().contains("parkour.checkpoints")) {
                plugin.getConfig().set("parkour.courses.easy.checkpoints", plugin.getConfig().get("parkour.checkpoints"));
                plugin.getConfig().set("parkour.checkpoints", null);
            }
            if (plugin.getConfig().contains("parkour.times")) {
                plugin.getConfig().set("parkour.courses.easy.times", plugin.getConfig().get("parkour.times"));
                plugin.getConfig().set("parkour.times", null);
            }
            if (plugin.getConfig().contains("parkour.hologram")) {
                plugin.getConfig().set("parkour.courses.easy.hologram", plugin.getConfig().get("parkour.hologram"));
                plugin.getConfig().set("parkour.hologram", null);
            }
            plugin.saveConfig();
        }

        courses.put("easy", easyCourse);
        courses.put("hard", hardCourse);

        // Load data for all courses
        for (ParkourCourse course : courses.values()) {
            String path = "parkour.courses." + course.getId();
            
            if (plugin.getConfig().contains(path + ".start")) {
                course.setStartPlate(plugin.getConfig().getLocation(path + ".start"));
            }
            if (plugin.getConfig().contains(path + ".end")) {
                course.setEndPlate(plugin.getConfig().getLocation(path + ".end"));
            }
            if (plugin.getConfig().contains(path + ".checkpoints")) {
                List<?> list = plugin.getConfig().getList(path + ".checkpoints");
                if (list != null) {
                    for (Object obj : list) {
                        if (obj instanceof Location) {
                            course.addCheckpoint((Location) obj);
                        }
                    }
                }
            }
            if (plugin.getConfig().contains(path + ".times")) {
                if (plugin.getDatabaseManager() != null && plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                    // Migrate config times to Redis and MySQL
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        for (String key : plugin.getConfig().getConfigurationSection(path + ".times").getKeys(false)) {
                            try {
                                UUID uuid = UUID.fromString(key);
                                long time = plugin.getConfig().getLong(path + ".times." + key);
                                
                                saveRecord(uuid, course.getId(), time);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        // Delete from config
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getConfig().set(path + ".times", null);
                            plugin.saveConfig();
                        });
                    });
                }
            }
            if (plugin.getConfig().contains(path + ".hologram")) {
                course.setHologramLocation(plugin.getConfig().getLocation(path + ".hologram"));
            }
        }
    }
    
    // Removed dead comment
    
    public void setStartPlate(String courseId, Location loc) {
        ParkourCourse course = courses.get(courseId);
        if (course != null) {
            course.setStartPlate(loc);
            plugin.getConfig().set("parkour.courses." + courseId + ".start", loc);
            plugin.saveConfig();
        }
    }
    
    public void setEndPlate(String courseId, Location loc) {
        ParkourCourse course = courses.get(courseId);
        if (course != null) {
            course.setEndPlate(loc);
            plugin.getConfig().set("parkour.courses." + courseId + ".end", loc);
            plugin.saveConfig();
        }
    }
    
    public void addCheckpoint(String courseId, Location loc) {
        ParkourCourse course = courses.get(courseId);
        if (course != null) {
            course.addCheckpoint(loc);
            plugin.getConfig().set("parkour.courses." + courseId + ".checkpoints", course.getCheckpoints());
            plugin.saveConfig();
        }
    }
    
    public void clearCheckpoints(String courseId) {
        ParkourCourse course = courses.get(courseId);
        if (course != null) {
            course.clearCheckpoints();
            plugin.getConfig().set("parkour.courses." + courseId + ".checkpoints", null);
            plugin.saveConfig();
        }
    }

    public void setHologramLocation(String courseId, Location loc) {
        ParkourCourse course = courses.get(courseId);
        if (course != null) {
            course.setHologramLocation(loc);
            plugin.getConfig().set("parkour.courses." + courseId + ".hologram", loc);
            plugin.saveConfig();
        }
    }

    public ParkourCourse getCourse(String id) {
        return courses.get(id);
    }
    
    public Collection<ParkourCourse> getCourses() {
        return courses.values();
    }
    
    private boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        return loc1.getBlockX() == loc2.getBlockX() && 
               loc1.getBlockY() == loc2.getBlockY() && 
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    public ParkourCourse getCourseByStartPlate(Location loc) {
        for (ParkourCourse course : courses.values()) {
            if (isSameBlock(course.getStartPlate(), loc)) {
                return course;
            }
        }
        return null;
    }
    
    public void hitCheckpoint(Player player, Location loc) {
        ActiveParkourSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        
        ParkourCourse course = session.getCourse();
        int cpIndex = -1;
        for (int i = 0; i < course.getCheckpoints().size(); i++) {
            if (isSameBlock(course.getCheckpoints().get(i), loc)) {
                cpIndex = i;
                break;
            }
        }
        
        if (cpIndex != -1) {
            if (cpIndex == session.getCurrentCheckpointIndex()) {
                session.setCurrentCheckpointIndex(cpIndex + 1);
                player.sendMessage(Constants.PREFIX + ChatColor.YELLOW + "Checkpoint " + ChatColor.WHITE + (cpIndex + 1) + "/" + course.getCheckpoints().size() + ChatColor.YELLOW + " atteint !");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                session.setLastCheckpointLocation(player.getLocation());
            }
        }
    }
    
    public void startParkour(Player player, ParkourCourse course) {
        if (isPlayerInModMode(player)) {
            return;
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            ActiveParkourSession currentSession = activeSessions.get(player.getUniqueId());
            if (currentSession.getCourse() == course) {
                if (System.currentTimeMillis() - currentSession.getStartTime() < 1000) {
                    return; // Anti-spam
                }
                player.sendMessage(Constants.PREFIX + ChatColor.YELLOW + "Parkour recommencé !");
            } else {
                player.sendMessage(Constants.PREFIX + ChatColor.YELLOW + "Nouveau parkour (" + course.getDisplayName() + ") commencé !");
            }
            cancelTimeout(player);
        } else {
            player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Parkour " + course.getDisplayName() + " commencé ! Vous avez " + ChatColor.WHITE + "2 minutes" + ChatColor.GREEN + ".");
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 2.0f);
        
        ActiveParkourSession newSession = new ActiveParkourSession(course, System.currentTimeMillis(), player.getLocation());
        activeSessions.put(player.getUniqueId(), newSession);
        
        giveReturnItem(player);
        
        // Schedule 2 minutes timeout (2 * 60 * 20 ticks)
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeSessions.containsKey(player.getUniqueId())) {
                cancelParkour(player);
                player.sendMessage(Constants.PREFIX + ChatColor.RED + "Temps écoulé " + ChatColor.GRAY + "(2 minutes)" + ChatColor.RED + " ! Parkour annulé.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }, 120 * 20L);
        timeoutTasks.put(player.getUniqueId(), task);
    }
    
    public void returnToStart(Player player) {
        ActiveParkourSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            Location loc = session.getLastCheckpointLocation();
            if (loc != null) {
                player.teleport(loc);
                
                if (session.getCurrentCheckpointIndex() == 0) {
                    startParkour(player, session.getCourse()); // Restart the timer if at start
                }
            }
        }
    }
    
    private void giveReturnItem(Player player) {
        ItemStack returnItem = new ItemStack(Material.RED_BED);
        ItemMeta meta = returnItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Retour au Checkpoint " + ChatColor.GRAY + "(Clic-Droit)");
            returnItem.setItemMeta(meta);
        }
        player.getInventory().setItem(0, returnItem);
        
        ItemStack quitItem = new ItemStack(Material.OAK_DOOR);
        ItemMeta quitMeta = quitItem.getItemMeta();
        if (quitMeta != null) {
            quitMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Quitter le Parkour " + ChatColor.GRAY + "(Clic-Droit)");
            quitItem.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(1, quitItem);
    }
    
    private void removeReturnItem(Player player) {
        player.getInventory().setItem(0, new ItemStack(Material.AIR));
        player.getInventory().setItem(1, new ItemStack(Material.AIR));
    }

    public void endParkour(Player player, Location endPlateLocation) {
        ActiveParkourSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            ParkourCourse course = session.getCourse();
            
            if (!isSameBlock(course.getEndPlate(), endPlateLocation)) {
                return; // Not the end plate of their current course
            }
            
            int requiredCheckpoints = course.getCheckpoints().size();
            int currentCp = session.getCurrentCheckpointIndex();
            
            if (currentCp < requiredCheckpoints) {
                player.sendMessage(Constants.PREFIX + ChatColor.RED + "Checkpoints manquants : " + ChatColor.WHITE + (requiredCheckpoints - currentCp) + "/" + requiredCheckpoints);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            cancelTimeout(player);
            removeReturnItem(player);
            
            long timeTaken = System.currentTimeMillis() - session.getStartTime();
            activeSessions.remove(player.getUniqueId());

            String formattedTime = String.format("%.2f", timeTaken / 1000.0);
            player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Bravo ! Parkour " + course.getDisplayName() + " terminé en " + ChatColor.YELLOW + formattedTime + "s" + ChatColor.GREEN + " !");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);

            if (plugin.getRedisManager() != null && plugin.getRedisManager().isConnected()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean isNewRecord = false;
                    try (redis.clients.jedis.Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                        Double currentBest = jedis.zscore("corehost:parkour:" + course.getId(), player.getUniqueId().toString());
                        
                        if (currentBest == null || timeTaken < currentBest) {
                            isNewRecord = true;
                            saveRecord(player.getUniqueId(), course.getId(), timeTaken);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    if (isNewRecord) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(Constants.PREFIX + ChatColor.GOLD + "✦ Nouveau record personnel !");
                            course.updateHologram();
                            
                            if (plugin.getScoreboardManager() != null) {
                                plugin.getScoreboardManager().updateScoreboard(player);
                            }
                        });
                    }
                });
            } else {
                player.sendMessage(Constants.PREFIX + ChatColor.RED + "Impossible d'enregistrer votre temps (Base de données hors-ligne).");
            }
        }
    }

    public void cancelParkour(Player player) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            cancelTimeout(player);
            removeReturnItem(player);
            activeSessions.remove(player.getUniqueId());
            player.sendMessage(Constants.PREFIX + ChatColor.RED + "Parkour annulé.");
        }
    }
    
    private void cancelTimeout(Player player) {
        BukkitTask task = timeoutTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public boolean isInParkour(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    public ActiveParkourSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    private void saveRecord(UUID uuid, String courseId, long time) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Insert to Redis
            try (redis.clients.jedis.Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                jedis.zadd("corehost:parkour:" + courseId, (double) time, uuid.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Insert to MySQL
            if (plugin.getDatabaseManager() != null) {
                try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                     java.sql.PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO parkour_records (uuid, course_id, best_time) VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE best_time = LEAST(best_time, ?)")) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, courseId);
                    stmt.setLong(3, time);
                    stmt.setLong(4, time);
                    stmt.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isPlayerInModMode(Player player) {
        org.bukkit.plugin.Plugin staffPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("CoreHost-StaffMod");
        if (staffPlugin != null && staffPlugin.isEnabled()) {
            try {
                Object modManager = staffPlugin.getClass().getMethod("getModManager").invoke(staffPlugin);
                if (modManager != null) {
                    Object result = modManager.getClass().getMethod("isModMode", UUID.class).invoke(modManager, player.getUniqueId());
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
