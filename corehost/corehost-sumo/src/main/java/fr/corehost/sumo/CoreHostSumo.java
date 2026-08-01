package fr.corehost.sumo;

import org.bukkit.plugin.java.JavaPlugin;

public class CoreHostSumo extends JavaPlugin {

    private SumoMapManager mapManager;
    private SumoGameManager gameManager;

    @Override
    public void onEnable() {
        getLogger().info("CoreHostSumo activation...");
        saveDefaultConfig();

        // Load map manager
        this.mapManager = new SumoMapManager(this);
        this.mapManager.loadMaps();

        // Initialize game manager
        this.gameManager = new SumoGameManager(this);

        // Register commands and listeners
        getCommand("sumosetup").setExecutor(new SumoSetupCommand(this));
        getCommand("sumosetup").setTabCompleter(new SumoSetupCommand(this));
        
        fr.corehost.sumo.commands.CoreHostSumoCommand command = new fr.corehost.sumo.commands.CoreHostSumoCommand(this);
        if (getCommand("corehostsumo") != null) getCommand("corehostsumo").setExecutor(command);
        
        getServer().getPluginManager().registerEvents(new SumoListener(this), this);

        getLogger().info("CoreHostSumo activated!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoreHostSumo deactivated.");
    }

    public SumoMapManager getMapManager() {
        return mapManager;
    }

    public SumoGameManager getGameManager() {
        return gameManager;
    }
}
