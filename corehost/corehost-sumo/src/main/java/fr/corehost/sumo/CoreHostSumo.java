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
        
        fr.corehost.sumo.commands.SumoCommand sumoCommand = new fr.corehost.sumo.commands.SumoCommand(this);
        if (getCommand("sumo") != null) getCommand("sumo").setExecutor(sumoCommand);
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new SumoListener(this), this);

        getLogger().info("CoreHostSumo activated!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoreHostSumo deactivated.");
        if (gameManager != null && fr.corehost.game.CoreHostGame.getInstance() != null && fr.corehost.game.CoreHostGame.getInstance().getRedisManager() != null) {
            fr.corehost.api.host.HostManager hostManager = new fr.corehost.api.host.HostManager(
                fr.corehost.game.CoreHostGame.getInstance().getRedisManager()
            );
            for (SumoGameInstance instance : gameManager.getActiveInstances()) {
                try {
                    hostManager.deleteHost(java.util.UUID.fromString(instance.getHostId())).join();
                } catch (Exception e) {
                    getLogger().warning("Failed to delete host from redis on shutdown: " + e.getMessage());
                }
                
                for (java.util.UUID uuid : instance.getPlayers()) {
                    org.bukkit.entity.Player p = getServer().getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        try {
                            com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                            out.writeUTF("Connect");
                            out.writeUTF("lobby");
                            p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    public SumoMapManager getMapManager() {
        return mapManager;
    }

    public SumoGameManager getGameManager() {
        return gameManager;
    }
}
