package fr.corehost.lobby.parkour;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import java.util.ArrayList;
import java.util.List;

public class ParkourHologram {

    private final Location location;
    private final List<ArmorStand> lines;

    public ParkourHologram(Location location) {
        this.location = location;
        this.lines = new ArrayList<>();
    }

    public void update(List<String> textLines) {
        clear();
        
        Location currentLoc = location.clone();
        for (String line : textLines) {
            ArmorStand as = (ArmorStand) location.getWorld().spawnEntity(currentLoc, EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setCustomNameVisible(true);
            as.setCustomName(line);
            as.setGravity(false);
            as.setMarker(true);
            as.addScoreboardTag("parkour_holo");
            
            lines.add(as);
            currentLoc.subtract(0, 0.25, 0);
        }
    }

    public void clear() {
        for (ArmorStand as : lines) {
            as.remove();
        }
        lines.clear();
        
        // Nettoyage de sécurité en cas de reload du serveur
        if (location.getWorld() != null) {
            for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, 2, 10, 2)) {
                if (entity instanceof ArmorStand && entity.getScoreboardTags().contains("parkour_holo")) {
                    entity.remove();
                }
            }
        }
    }
}
