package fr.corehost.sumo;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SumoMapManager {

    private final CoreHostSumo plugin;
    private final File mapsFile;
    private FileConfiguration mapsConfig;
    private final Map<String, SumoMapConfig> loadedMaps = new HashMap<>();

    public SumoMapManager(CoreHostSumo plugin) {
        this.plugin = plugin;
        this.mapsFile = new File(plugin.getDataFolder(), "sumo_maps.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!mapsFile.exists()) {
            try {
                mapsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create sumo_maps.yml!");
            }
        }
        this.mapsConfig = YamlConfiguration.loadConfiguration(mapsFile);
    }

    public void loadMaps() {
        loadedMaps.clear();
        for (String key : mapsConfig.getKeys(false)) {
            SumoMapConfig mapConfig = new SumoMapConfig(key);
            mapConfig.load(mapsConfig.getConfigurationSection(key));
            loadedMaps.put(key.toLowerCase(), mapConfig);
        }
        plugin.getLogger().info("Loaded " + loadedMaps.size() + " sumo maps.");
    }

    public void saveMap(SumoMapConfig mapConfig) {
        loadedMaps.put(mapConfig.getName().toLowerCase(), mapConfig);
        org.bukkit.configuration.ConfigurationSection section = mapsConfig.getConfigurationSection(mapConfig.getName());
        if (section == null) {
            section = mapsConfig.createSection(mapConfig.getName());
        }
        mapConfig.save(section);

        
        try {
            mapsConfig.save(mapsFile);
            plugin.getLogger().info("Saved map " + mapConfig.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save map " + mapConfig.getName() + " to sumo_maps.yml!");
        }
    }

    public SumoMapConfig getMap(String name) {
        return loadedMaps.get(name.toLowerCase());
    }

    public SumoMapConfig getOrCreateMap(String name) {
        SumoMapConfig map = getMap(name);
        if (map == null) {
            map = new SumoMapConfig(name);
        }
        return map;
    }

    public Map<String, SumoMapConfig> getLoadedMaps() {
        return loadedMaps;
    }
}
