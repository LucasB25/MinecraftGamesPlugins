package fr.corehost.dac;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

public class DacMapConfig {
    
    private String name;
    private Location divingBoardSpawn;
    private Location spectatorSpawn;
    
    // Pool area definition
    private Location poolMin;
    private Location poolMax;

    private String divingBoardWorldName;
    private String spectatorWorldName;
    private String explicitTemplateName;

    public DacMapConfig(String name) {
        this.name = name;
    }

    public void load(ConfigurationSection section) {
        this.explicitTemplateName = section.getString("template");
        
        if (section.contains("divingBoard")) {
            this.divingBoardSpawn = loadLocation(section.getConfigurationSection("divingBoard"), true);
        }
        
        if (section.contains("spectatorSpawn")) {
            this.spectatorSpawn = loadLocation(section.getConfigurationSection("spectatorSpawn"), false);
        }
        
        if (section.contains("pool.min")) {
            this.poolMin = loadLocation(section.getConfigurationSection("pool.min"), false);
        }
        if (section.contains("pool.max")) {
            this.poolMax = loadLocation(section.getConfigurationSection("pool.max"), false);
        }
    }

    public void save(ConfigurationSection section) {
        section.set("template", getTemplateName());
        
        if (divingBoardSpawn != null) {
            saveLocation(section.createSection("divingBoard"), divingBoardSpawn);
        }
        if (spectatorSpawn != null) {
            saveLocation(section.createSection("spectatorSpawn"), spectatorSpawn);
        }
        if (poolMin != null) {
            saveLocation(section.createSection("pool.min"), poolMin);
        }
        if (poolMax != null) {
            saveLocation(section.createSection("pool.max"), poolMax);
        }
    }

    private void saveLocation(ConfigurationSection section, Location loc) {
        section.set("world", getTemplateName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    private Location loadLocation(ConfigurationSection section, boolean isDivingBoard) {
        if (section == null) return null;
        String worldName = section.getString("world", "world");
        if (isDivingBoard) divingBoardWorldName = worldName;
        else spectatorWorldName = worldName;

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    public String getName() {
        return name;
    }

    public String getTemplateName() {
        if (explicitTemplateName != null) {
            return explicitTemplateName;
        }
        if (divingBoardWorldName != null) {
            return divingBoardWorldName;
        }
        if (divingBoardSpawn != null && divingBoardSpawn.getWorld() != null) {
            return divingBoardSpawn.getWorld().getName();
        }
        return name;
    }

    public Location getDivingBoardSpawn() {
        if (divingBoardSpawn != null && divingBoardSpawn.getWorld() == null && divingBoardWorldName != null) {
            divingBoardSpawn.setWorld(org.bukkit.Bukkit.getWorld(divingBoardWorldName));
        }
        return divingBoardSpawn;
    }

    public void setDivingBoardSpawn(Location divingBoardSpawn) {
        this.divingBoardSpawn = divingBoardSpawn;
        if (divingBoardSpawn != null && divingBoardSpawn.getWorld() != null) {
            this.divingBoardWorldName = divingBoardSpawn.getWorld().getName();
        }
    }

    public Location getSpectatorSpawn() {
        if (spectatorSpawn != null && spectatorSpawn.getWorld() == null && spectatorWorldName != null) {
            spectatorSpawn.setWorld(org.bukkit.Bukkit.getWorld(spectatorWorldName));
        }
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
        if (spectatorSpawn != null && spectatorSpawn.getWorld() != null) {
            this.spectatorWorldName = spectatorSpawn.getWorld().getName();
        }
    }

    public Location getPoolMin() {
        return poolMin;
    }

    public void setPoolMin(Location poolMin) {
        this.poolMin = poolMin;
    }

    public Location getPoolMax() {
        return poolMax;
    }

    public void setPoolMax(Location poolMax) {
        this.poolMax = poolMax;
    }
    
    public BoundingBox getPoolBounds() {
        if (poolMin == null || poolMax == null) return null;
        return BoundingBox.of(poolMin, poolMax);
    }
    
    public int getPoolYLevel() {
        if (poolMin != null) return poolMin.getBlockY();
        return 0;
    }

    public boolean isSetup() {
        boolean hasDivingBoard = this.divingBoardSpawn != null || this.divingBoardWorldName != null;
        boolean hasPool = this.poolMin != null && this.poolMax != null;
        return hasDivingBoard && hasPool;
    }
}
