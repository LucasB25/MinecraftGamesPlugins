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
import java.util.stream.Collectors;

public class ParkourManager {

    private final Map<UUID, Long> activeSessions;
    private final Map<UUID, Long> bestTimes;
    private final Map<UUID, BukkitTask> timeoutTasks;
    private final Map<UUID, Location> startLocations;
    private final Map<UUID, Integer> playerCheckpoints;
    
    private Location startPlate;
    private Location endPlate;
    private final List<Location> checkpoints;
    
    private final CoreHostLobby plugin;
    private ParkourHologram hologram;

    public ParkourManager(CoreHostLobby plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        this.bestTimes = new HashMap<>();
        this.timeoutTasks = new HashMap<>();
        this.startLocations = new HashMap<>();
        this.playerCheckpoints = new HashMap<>();
        this.checkpoints = new ArrayList<>();
        
        loadConfigData();
        loadHologramLocation();
    }
    
    public void loadConfigData() {
        checkpoints.clear();
        if (plugin.getConfig().contains("parkour.start")) {
            startPlate = plugin.getConfig().getLocation("parkour.start");
        }
        if (plugin.getConfig().contains("parkour.end")) {
            endPlate = plugin.getConfig().getLocation("parkour.end");
        }
        if (plugin.getConfig().contains("parkour.checkpoints")) {
            List<?> list = plugin.getConfig().getList("parkour.checkpoints");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof Location) {
                        checkpoints.add((Location) obj);
                    }
                }
            }
        }
        
        // Load best times
        bestTimes.clear();
        if (plugin.getConfig().contains("parkour.times")) {
            for (String key : plugin.getConfig().getConfigurationSection("parkour.times").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long time = plugin.getConfig().getLong("parkour.times." + key);
                    bestTimes.put(uuid, time);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
    
    public void saveTimes() {
        plugin.getConfig().set("parkour.times", null); // Clear old times
        for (Map.Entry<UUID, Long> entry : bestTimes.entrySet()) {
            plugin.getConfig().set("parkour.times." + entry.getKey().toString(), entry.getValue());
        }
        plugin.saveConfig();
    }
    
    public void setStartPlate(Location loc) {
        startPlate = loc;
        plugin.getConfig().set("parkour.start", loc);
        plugin.saveConfig();
    }
    
    public void setEndPlate(Location loc) {
        endPlate = loc;
        plugin.getConfig().set("parkour.end", loc);
        plugin.saveConfig();
    }
    
    public void addCheckpoint(Location loc) {
        checkpoints.add(loc);
        plugin.getConfig().set("parkour.checkpoints", checkpoints);
        plugin.saveConfig();
    }
    
    public void clearCheckpoints() {
        checkpoints.clear();
        plugin.getConfig().set("parkour.checkpoints", null);
        plugin.saveConfig();
    }
    
    public boolean isStartPlate(Location loc) {
        return startPlate != null && startPlate.getBlockX() == loc.getBlockX() && startPlate.getBlockY() == loc.getBlockY() && startPlate.getBlockZ() == loc.getBlockZ();
    }
    
    public boolean isEndPlate(Location loc) {
        return endPlate != null && endPlate.getBlockX() == loc.getBlockX() && endPlate.getBlockY() == loc.getBlockY() && endPlate.getBlockZ() == loc.getBlockZ();
    }
    
    public int getCheckpointIndex(Location loc) {
        for (int i = 0; i < checkpoints.size(); i++) {
            Location cp = checkpoints.get(i);
            if (cp.getBlockX() == loc.getBlockX() && cp.getBlockY() == loc.getBlockY() && cp.getBlockZ() == loc.getBlockZ()) {
                return i;
            }
        }
        return -1;
    }
    
    public void hitCheckpoint(Player player, Location loc) {
        if (!isInParkour(player)) return;
        
        int cpIndex = getCheckpointIndex(loc);
        if (cpIndex != -1) {
            int currentCp = playerCheckpoints.getOrDefault(player.getUniqueId(), 0);
            if (cpIndex == currentCp) {
                playerCheckpoints.put(player.getUniqueId(), currentCp + 1);
                player.sendMessage(Constants.PREFIX + ChatColor.YELLOW + "Checkpoint " + ChatColor.WHITE + (currentCp + 1) + "/" + checkpoints.size() + ChatColor.YELLOW + " atteint !");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                // Mettre à jour la startLocation pour pouvoir y retourner
                startLocations.put(player.getUniqueId(), player.getLocation());
            }
        }
    }
    
    public void loadHologramLocation() {
        if (plugin.getConfig().contains("parkour.hologram")) {
            Location loc = plugin.getConfig().getLocation("parkour.hologram");
            if (loc != null) {
                if (hologram != null) hologram.clear();
                hologram = new ParkourHologram(loc);
                updateHologram();
            }
        }
    }
    
    public void setHologramLocation(Location loc) {
        plugin.getConfig().set("parkour.hologram", loc);
        plugin.saveConfig();
        loadHologramLocation();
    }

    public void startParkour(Player player) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            long lastStart = activeSessions.get(player.getUniqueId());
            if (System.currentTimeMillis() - lastStart < 1000) {
                return; // Anti-spam (empêche le double appel lit + plaque)
            }
            player.sendMessage(Constants.PREFIX + ChatColor.YELLOW + "Parkour recommencé !");
            cancelTimeout(player);
        } else {
            player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Parkour commencé ! Vous avez " + ChatColor.WHITE + "2 minutes" + ChatColor.GREEN + ".");
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 2.0f);
        
        activeSessions.put(player.getUniqueId(), System.currentTimeMillis());
        startLocations.put(player.getUniqueId(), player.getLocation());
        playerCheckpoints.put(player.getUniqueId(), 0);
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
        if (isInParkour(player)) {
            Location startLoc = startLocations.get(player.getUniqueId());
            if (startLoc != null) {
                player.teleport(startLoc);
                
                int currentCp = playerCheckpoints.getOrDefault(player.getUniqueId(), 0);
                if (currentCp == 0) {
                    startParkour(player); // Restart the timer only if at the start
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
            quitMeta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Quitter le Parkour " + ChatColor.GRAY + "(Clic-Droit)");
            quitItem.setItemMeta(quitMeta);
        }
        player.getInventory().setItem(1, quitItem);
    }
    
    private void removeReturnItem(Player player) {
        player.getInventory().setItem(0, new ItemStack(Material.AIR));
        player.getInventory().setItem(1, new ItemStack(Material.AIR));
    }

    public void endParkour(Player player) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            int requiredCheckpoints = checkpoints.size();
            int currentCp = playerCheckpoints.getOrDefault(player.getUniqueId(), 0);
            
            if (currentCp < requiredCheckpoints) {
                player.sendMessage(Constants.PREFIX + ChatColor.RED + "Checkpoints manquants : " + ChatColor.WHITE + (requiredCheckpoints - currentCp) + "/" + requiredCheckpoints);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            cancelTimeout(player);
            removeReturnItem(player);
            startLocations.remove(player.getUniqueId());
            playerCheckpoints.remove(player.getUniqueId());
            
            long startTime = activeSessions.get(player.getUniqueId());
            long timeTaken = System.currentTimeMillis() - startTime;
            activeSessions.remove(player.getUniqueId());

            String formattedTime = String.format("%.2f", timeTaken / 1000.0);
            player.sendMessage(Constants.PREFIX + ChatColor.GREEN + "Bravo ! Parkour terminé en " + ChatColor.YELLOW + formattedTime + "s" + ChatColor.GREEN + " !");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);

            if (!bestTimes.containsKey(player.getUniqueId()) || bestTimes.get(player.getUniqueId()) > timeTaken) {
                bestTimes.put(player.getUniqueId(), timeTaken);
                player.sendMessage(Constants.PREFIX + ChatColor.GOLD + "✦ Nouveau record personnel !");
                saveTimes();
                updateHologram();
            }
        }
    }

    public void cancelParkour(Player player) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            cancelTimeout(player);
            removeReturnItem(player);
            startLocations.remove(player.getUniqueId());
            playerCheckpoints.remove(player.getUniqueId());
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
    
    private void updateHologram() {
        if (hologram == null) return;
        
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Top 10 Parkour ✦");
        lines.add("");
        
        if (bestTimes.isEmpty()) {
            lines.add(ChatColor.GRAY + "Aucun record pour l'instant.");
        } else {
            List<Map.Entry<UUID, Long>> sorted = bestTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(10)
                .collect(Collectors.toList());
                
            int rank = 1;
            for (Map.Entry<UUID, Long> entry : sorted) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "Inconnu";
                String formattedTime = String.format("%.2f", entry.getValue() / 1000.0);
                
                ChatColor rankColor;
                if (rank == 1) rankColor = ChatColor.GOLD;
                else if (rank == 2) rankColor = ChatColor.GRAY;
                else if (rank == 3) rankColor = ChatColor.RED;
                else rankColor = ChatColor.DARK_GRAY;
                
                lines.add(rankColor + "#" + rank + " " + ChatColor.WHITE + name + ChatColor.DARK_GRAY + " - " + ChatColor.GREEN + formattedTime + "s");
                rank++;
            }
        }
        
        lines.add("");
        hologram.update(lines);
    }
    
    public boolean isInParkour(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    public Map<UUID, Long> getBestTimes() {
        return bestTimes;
    }
}
