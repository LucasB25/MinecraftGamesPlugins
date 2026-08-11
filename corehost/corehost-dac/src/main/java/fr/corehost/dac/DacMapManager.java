package fr.corehost.dac;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DacMapManager {

    private final CoreHostDac plugin;
    private final File mapsFile;
    private FileConfiguration mapsConfig;
    private final Map<String, DacMapConfig> loadedMaps = new HashMap<>();

    public DacMapManager(CoreHostDac plugin) {
        this.plugin = plugin;
        this.mapsFile = new File(plugin.getDataFolder(), "dac_maps.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!mapsFile.exists()) {
            try {
                mapsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create dac_maps.yml!");
            }
        }
        this.mapsConfig = YamlConfiguration.loadConfiguration(mapsFile);
    }

    public void loadMaps() {
        loadedMaps.clear();
        for (String key : mapsConfig.getKeys(false)) {
            DacMapConfig mapConfig = new DacMapConfig(key);
            mapConfig.load(mapsConfig.getConfigurationSection(key));
            loadedMaps.put(key.toLowerCase(), mapConfig);
        }
        plugin.getLogger().info("Loaded " + loadedMaps.size() + " dac maps.");
    }

    public void reloadMaps() {
        this.mapsConfig = YamlConfiguration.loadConfiguration(mapsFile);
        loadMaps();
    }

    public void saveMap(DacMapConfig mapConfig) {
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
            plugin.getLogger().severe("Could not save map " + mapConfig.getName() + " to dac_maps.yml!");
        }
    }

    public DacMapConfig getMap(String name) {
        return loadedMaps.get(name.toLowerCase());
    }

    public DacMapConfig getOrCreateMap(String name) {
        DacMapConfig map = getMap(name);
        if (map == null) {
            map = new DacMapConfig(name);
        }
        return map;
    }

    public Map<String, DacMapConfig> getLoadedMaps() {
        return loadedMaps;
    }

    public DacMapConfig getRandomFunctionalMap() {
        java.util.List<DacMapConfig> functional = new java.util.ArrayList<>();
        for (DacMapConfig map : loadedMaps.values()) {
            if (map.isSetup()) {
                functional.add(map);
            }
        }
        if (functional.isEmpty()) return null;
        return functional.get(new java.util.Random().nextInt(functional.size()));
    }
}
