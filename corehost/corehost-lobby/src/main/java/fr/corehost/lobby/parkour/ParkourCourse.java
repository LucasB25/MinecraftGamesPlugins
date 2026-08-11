package fr.corehost.lobby.parkour;

import org.bukkit.Bukkit;
import fr.corehost.api.utils.CC;
import org.bukkit.Location;

import fr.corehost.lobby.CoreHostLobby;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ParkourCourse {

    private final CoreHostLobby plugin;
    private final String id;
    private final String displayName;
    
    private Location startPlate;
    private Location endPlate;
    private final List<Location> checkpoints;
    
    private Location hologramLocation;
    private ParkourHologram hologram;

    public ParkourCourse(CoreHostLobby plugin, String id, String displayName) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.checkpoints = new ArrayList<>();
    }

    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public Location getStartPlate() {
        return startPlate;
    }

    public void setStartPlate(Location startPlate) {
        this.startPlate = startPlate;
    }

    public Location getEndPlate() {
        return endPlate;
    }

    public void setEndPlate(Location endPlate) {
        this.endPlate = endPlate;
    }

    public List<Location> getCheckpoints() {
        return checkpoints;
    }
    
    public void addCheckpoint(Location loc) {
        this.checkpoints.add(loc);
    }
    
    public void clearCheckpoints() {
        this.checkpoints.clear();
    }

    public Location getHologramLocation() {
        return hologramLocation;
    }

    public void setHologramLocation(Location hologramLocation) {
        this.hologramLocation = hologramLocation;
        if (hologramLocation != null) {
            if (this.hologram != null) {
                this.hologram.clear();
            }
            this.hologram = new ParkourHologram(hologramLocation);
            updateHologram();
        } else if (this.hologram != null) {
            this.hologram.clear();
            this.hologram = null;
        }
    }
    
    public void reloadHologram() {
        if (hologramLocation != null) {
            if (hologram != null) hologram.clear();
            hologram = new ParkourHologram(hologramLocation);
            updateHologram();
        }
    }

    public void updateHologram() {
        if (hologram == null) return;
        
        if (plugin.getRedisManager() == null || !plugin.getRedisManager().isConnected()) {
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Tuple> top10 = new ArrayList<>();
            try (Jedis jedis = plugin.getRedisManager().getPool().getResource()) {
                List<Tuple> results = jedis.zrangeWithScores("corehost:parkour:" + id, 0, 9);
                if (results != null) {
                    top10.addAll(results);
                }
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> lines = new ArrayList<>();
                lines.add("");
                lines.add(CC.GOLD + "" + CC.BOLD + "✦ Top 10 Parkour - " + displayName + " ✦");
                lines.add(CC.DARK_GRAY + "" + CC.STRIKETHROUGH + "                    ");

                if (top10.isEmpty()) {
                    lines.add(CC.GRAY + "Aucun record pour l'instant.");
                } else {
                    int rank = 1;
                    for (Tuple tuple : top10) {
                        String uuidStr = tuple.getElement();
                        long timeTaken = (long) tuple.getScore();
                        
                        String name = "Inconnu";
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            name = Bukkit.getOfflinePlayer(uuid).getName();
                            if (name == null) name = "Inconnu";
                        } catch (Exception ignored) {}
                        
                        String formattedTime = String.format("%.2f", timeTaken / 1000.0);

                        String rankColor;
                        if (rank == 1) rankColor = CC.GOLD;
                        else if (rank == 2) rankColor = CC.GRAY;
                        else if (rank == 3) rankColor = CC.RED;
                        else rankColor = CC.GRAY;

                        lines.add(rankColor + "#" + rank + " " + CC.WHITE + name + CC.DARK_GRAY + " - " + CC.GREEN + formattedTime + "s");
                        rank++;
                    }
                }

                lines.add("");
                hologram.update(lines);
            });
        });
    }
}
