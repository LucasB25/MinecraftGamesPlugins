package fr.corehost.sumo;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class SumoMapConfig {
    
    private String name;
    private Location spawn1;
    private Location spawn2;
    private int deathHeight;

    public SumoMapConfig(String name) {
        this.name = name;
        this.deathHeight = 0; // default
    }

    private String spawn1WorldName;
    private String spawn2WorldName;

    public void load(ConfigurationSection section) {
        if (section.contains("spawn1.world")) {
            this.spawn1 = loadLocation(section.getConfigurationSection("spawn1"), 1);
        } else if (section.contains("spawn1")) {
            // Fallback for old bukkit serialization
            try { this.spawn1 = section.getLocation("spawn1"); } catch(Exception e) {}
        }
        
        if (section.contains("spawn2.world")) {
            this.spawn2 = loadLocation(section.getConfigurationSection("spawn2"), 2);
        } else if (section.contains("spawn2")) {
            // Fallback for old bukkit serialization
            try { this.spawn2 = section.getLocation("spawn2"); } catch(Exception e) {}
        }
        
        this.deathHeight = section.getInt("deathHeight", 0);
    }

    public void save(ConfigurationSection section) {
        if (spawn1 != null) {
            saveLocation(section.createSection("spawn1"), spawn1);
        }
        if (spawn2 != null) {
            saveLocation(section.createSection("spawn2"), spawn2);
        }
        section.set("deathHeight", deathHeight);
    }

    private void saveLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    private Location loadLocation(ConfigurationSection section, int id) {
        if (section == null) return null;
        String worldName = section.getString("world", "world");
        if (id == 1) spawn1WorldName = worldName;
        if (id == 2) spawn2WorldName = worldName;

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    public String getName() {
        return name;
    }

    public Location getSpawn1() {
        if (spawn1 != null && spawn1.getWorld() == null && spawn1WorldName != null) {
            spawn1.setWorld(org.bukkit.Bukkit.getWorld(spawn1WorldName));
        }
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
        if (spawn1 != null && spawn1.getWorld() != null) this.spawn1WorldName = spawn1.getWorld().getName();
    }

    public Location getSpawn2() {
        if (spawn2 != null && spawn2.getWorld() == null && spawn2WorldName != null) {
            spawn2.setWorld(org.bukkit.Bukkit.getWorld(spawn2WorldName));
        }
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
        if (spawn2 != null && spawn2.getWorld() != null) this.spawn2WorldName = spawn2.getWorld().getName();
    }

    public int getDeathHeight() {
        return deathHeight;
    }

    public void setDeathHeight(int deathHeight) {
        this.deathHeight = deathHeight;
    }

    public boolean isSetup() {
        boolean hasSpawn1 = this.spawn1 != null || this.spawn1WorldName != null;
        boolean hasSpawn2 = this.spawn2 != null || this.spawn2WorldName != null;
        return hasSpawn1 && hasSpawn2;
    }
}
