package fr.corehost.dac;

import org.bukkit.plugin.java.JavaPlugin;

public class CoreHostDac extends JavaPlugin {

    private DacMapManager mapManager;
    private DacGameManager gameManager;

    @Override
    public void onEnable() {
        getLogger().info("CoreHostDac activation...");
        saveDefaultConfig();

        // Load map manager
        this.mapManager = new DacMapManager(this);
        this.mapManager.loadMaps();

        // Initialize game manager
        this.gameManager = new DacGameManager(this);

        // Register commands and listeners
        getCommand("dacsetup").setExecutor(new fr.corehost.dac.commands.DacSetupCommand(this));
        getCommand("dacsetup").setTabCompleter(new fr.corehost.dac.commands.DacSetupCommand(this));
        
        fr.corehost.dac.commands.CoreHostDacCommand command = new fr.corehost.dac.commands.CoreHostDacCommand(this);
        if (getCommand("corehostdac") != null) getCommand("corehostdac").setExecutor(command);
        
        fr.corehost.dac.commands.DacCommand dacCommand = new fr.corehost.dac.commands.DacCommand(this);
        if (getCommand("dac") != null) getCommand("dac").setExecutor(dacCommand);
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new DacListener(this), this);

        getLogger().info("CoreHostDac activated!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoreHostDac deactivated.");
    }

    public DacMapManager getMapManager() {
        return mapManager;
    }

    public DacGameManager getGameManager() {
        return gameManager;
    }
}
