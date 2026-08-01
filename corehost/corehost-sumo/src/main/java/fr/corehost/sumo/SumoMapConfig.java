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

    public void load(ConfigurationSection section) {
        if (section.contains("spawn1")) {
            this.spawn1 = section.getLocation("spawn1");
        }
        if (section.contains("spawn2")) {
            this.spawn2 = section.getLocation("spawn2");
        }
        this.deathHeight = section.getInt("deathHeight", 0);
    }

    public void save(ConfigurationSection section) {
        if (spawn1 != null) {
            section.set("spawn1", spawn1);
        }
        if (spawn2 != null) {
            section.set("spawn2", spawn2);
        }
        section.set("deathHeight", deathHeight);
    }

    public String getName() {
        return name;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public int getDeathHeight() {
        return deathHeight;
    }

    public void setDeathHeight(int deathHeight) {
        this.deathHeight = deathHeight;
    }
}
